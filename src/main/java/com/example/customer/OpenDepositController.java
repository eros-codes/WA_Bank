package com.example.customer;

import com.example.bank.model.Money;
import com.example.bank.model.TermDeposit;
import com.example.bank.ui.Dialogs;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.TextFlow;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Opening a deposit: choose the kind, its term and how often interest is
 * paid, then pay from one of your accounts. Foreign-currency and
 * interest-free deposits were designed but never worked (their buttons only
 * printed "hi"); they work now.
 */
public class OpenDepositController extends CustomerPage {

    private enum Kind { SHORT, LONG, FOREIGN, FREE }

    @FXML private AnchorPane create, shortterm, longterm, foreigncurrency, interestfree, payment;
    @FXML private Button shortd, longd, foreignd, intrestfreed, backtomenu, back1, back2, back3, back4, accback;
    @FXML private Button onemonthshort, threemonthshort, sixmonthshort, oneyearshort, shortonemonthinterest, shortthreemonthinterest, shortoneyearinterest, shortdone;
    @FXML private Button oneyearlong, twoyearlong, threeyearlong, fiveyearlong, longonemonthinterest, longthreemonthinterest, longoneyearinterest, longdone;
    @FXML private Button oneyearforeign, threeyearforeign, fiveyearforeign, foreignonemonthinterest, foreignthreemonthinterest, foreignoneyearinterest, foreigndone, dollar, euro;
    @FXML private Button oneyearfree, twoyearfree, threeyearfree, fiveyearfree, freedone;
    @FXML private CheckBox shortaccept, longaccept, foregnaccept, freeaccept;
    @FXML private Label shortinterest, longinterest, foreigninterest, freeinterest, depositamount;
    @FXML private TextField shortamount, longamount, foreignamount, freeamount, card, getotp;
    @FXML private TextFlow card1;
    @FXML private Button sendotp, donepayment;

    private final Map<String, AnchorPane> panes = new LinkedHashMap<>();
    private Kind kind;
    private String typeLabel;
    private String currency;
    private String duration;
    private String interest;
    private String payout;
    private TermDeposit pending;
    private OtpChallenge otp;

    @Override
    protected void onLoad() {
        for (AnchorPane p : List.of(create, shortterm, longterm, foreigncurrency, interestfree, payment)) panes.put(p.getId(), p);
        show("create");
        homeButtons(backtomenu);
        for (Button b : new Button[]{back1, back2, back3, back4, accback}) b.setOnAction(e -> show("create"));
        start(shortd, Kind.SHORT, "IRR", "shortterm");
        start(longd, Kind.LONG, "IRR", "longterm");
        start(foreignd, Kind.FOREIGN, null, "foreigncurrency");
        start(intrestfreed, Kind.FREE, "IRR", "interestfree");

        term(shortinterest, new Button[]{onemonthshort, threemonthshort, sixmonthshort, oneyearshort},
                new String[]{"1 month", "3 months", "6 months", "1 year"}, new String[]{"5%", "7%", "9%", "12%"});
        term(longinterest, new Button[]{oneyearlong, twoyearlong, threeyearlong, fiveyearlong},
                new String[]{"1 year", "2 years", "3 years", "5 years"}, new String[]{"10%", "12%", "15%", "20%"});
        term(freeinterest, new Button[]{oneyearfree, twoyearfree, threeyearfree, fiveyearfree},
                new String[]{"1 year", "2 years", "3 years", "5 years"}, new String[]{"0%", "0%", "0%", "0%"});
        Button[] foreignTerms = {oneyearforeign, threeyearforeign, fiveyearforeign};
        String[] foreignDurations = {"1 year", "3 years", "5 years"};
        for (int i = 0; i < 3; i++) {
            String d = foreignDurations[i];
            Button b = foreignTerms[i];
            b.setOnAction(e -> {
                duration = d;
                exclusive(foreignTerms, b);
                showForeignInterest();
            });
        }
        pick(new Button[]{dollar, euro}, new String[]{"USD", "EUR"}, v -> {
            currency = v;
            showForeignInterest();
        });
        for (Button[] set : new Button[][]{{shortonemonthinterest, shortthreemonthinterest, shortoneyearinterest},
                {longonemonthinterest, longthreemonthinterest, longoneyearinterest},
                {foreignonemonthinterest, foreignthreemonthinterest, foreignoneyearinterest}}) {
            pick(set, new String[]{"1 month", "3 months", "12 months"}, v -> payout = v);
        }
        for (TextField f : new TextField[]{shortamount, longamount, foreignamount, freeamount}) Inputs.digits(f, 15);
        shortdone.setOnAction(e -> review(shortamount, shortaccept));
        longdone.setOnAction(e -> review(longamount, longaccept));
        foreigndone.setOnAction(e -> review(foreignamount, foregnaccept));
        freedone.setOnAction(e -> review(freeamount, freeaccept));

        var mine = Dialogs.attempt(() -> app().customers().primaryAccount(me()).orElse(null));
        SuggestionField.attach(card, card1, mine == null ? null : mine.accountNumber(), () -> { }, () -> { });
        otp = new OtpChallenge(sendotp, new Label());
        donepayment.setOnAction(e -> pay());
    }

    private void show(String pane) {
        panes.forEach((id, p) -> p.setVisible(id.equals(pane)));
    }

    private void start(Button b, Kind k, String fixedCurrency, String pane) {
        b.setOnAction(e -> {
            kind = k;
            typeLabel = b.getText();
            currency = fixedCurrency;
            duration = null;
            interest = k == Kind.FREE ? "0%" : null;
            payout = null;
            show(pane);
        });
    }

    private void term(Label shown, Button[] buttons, String[] durations, String[] rates) {
        for (int i = 0; i < buttons.length; i++) {
            String d = durations[i], r = rates[i];
            Button b = buttons[i];
            b.setOnAction(e -> {
                duration = d;
                interest = r;
                shown.setText(r);
                exclusive(buttons, b);
            });
        }
    }

    /** The foreign rate depends on both the currency and the term, so it is shown once both are chosen. */
    private void showForeignInterest() {
        if (currency == null || duration == null) return; // the old screen crashed here when the currency was not chosen yet
        interest = switch (currency + " " + duration) {
            case "EUR 1 year" -> "2%";
            case "EUR 3 years" -> "3%";
            case "EUR 5 years" -> "5%";
            case "USD 1 year" -> "1%";
            case "USD 3 years" -> "2%";
            case "USD 5 years" -> "4%";
            default -> null;
        };
        foreigninterest.setText(interest);
    }

    private static void pick(Button[] buttons, String[] values, java.util.function.Consumer<String> record) {
        for (int i = 0; i < buttons.length; i++) {
            String v = values[i];
            Button b = buttons[i];
            b.setOnAction(e -> {
                record.accept(v);
                exclusive(buttons, b);
            });
        }
    }

    private static void exclusive(Button[] buttons, Button chosen) {
        for (Button b : buttons) b.setDisable(b == chosen);
    }

    private void review(TextField amountField, CheckBox accepted) {
        long minimum = Long.parseLong("0" + amountField.getPromptText().replaceAll("\\D", ""));
        if (amountField.getText().isBlank() || Long.parseLong(amountField.getText()) < minimum) {
            Dialogs.error("Enter at least " + Money.of(minimum).format() + (kind == Kind.FOREIGN ? "" : " IRR") + ".");
            amountField.setStyle("-fx-border-color: red; -fx-border-width: 2px; -fx-border-radius: 15px;");
            return;
        }
        amountField.setStyle("");
        if (currency == null) { Dialogs.error("Choose the deposit's currency."); return; }
        if (duration == null) { Dialogs.error("Choose how long the deposit runs."); return; }
        if (kind != Kind.FREE && payout == null) { Dialogs.error("Choose how often interest is paid."); return; }
        if (!accepted.isSelected()) { Dialogs.error("Please accept the terms."); return; }
        pending = new TermDeposit(0, me(), null, typeLabel, Money.parse(amountField.getText()), duration, interest,
                kind == Kind.FREE ? "at the end" : payout, currency, LocalDate.now().toString());
        String inRials = pending.valueInRials().format() + " IRR";
        depositamount.setText("IRR".equals(currency) ? inRials : pending.amount().format() + " " + currency + " = " + inRials);
        show("payment");
    }

    private void pay() {
        if (!otp.accept(getotp.getText())) {
            Dialogs.error("The one-time code is wrong or has expired.");
            return;
        }
        TermDeposit opened = Dialogs.attempt(() -> app().deposits().open(me(), card.getText(), pending));
        if (opened == null) return;
        otp.reset();
        Dialogs.info("Deposit opened", "Your deposit number is " + opened.number() + ".");
        open(donepayment, CustomerScreen.MY_DEPOSITS);
    }
}
