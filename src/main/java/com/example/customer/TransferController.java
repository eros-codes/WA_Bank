package com.example.customer;

import com.example.bank.model.Account;
import com.example.bank.model.BankDirectory;
import com.example.bank.model.IbanMethod;
import com.example.bank.model.LedgerEntry;
import com.example.bank.model.Money;
import com.example.bank.model.Recipient;
import com.example.bank.service.InsufficientFundsException;
import com.example.bank.service.TransferService;
import com.example.bank.ui.Dialogs;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.TextFlow;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The three ways to send money: card to card, account to account (by
 * dragging one account onto the other) and IBAN. The screen only collects
 * input and shows results; the transfer itself happens in TransferService,
 * as one transaction.
 */
public class TransferController extends CustomerPage implements Sectioned {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // sections
    @FXML private AnchorPane cardtocard, cardotp, cardrecipe, accounttransfer, accountrecipe, ibantransfer, ibanrecipe;
    // card to card
    @FXML private TextField sourcecard, cardyear, cardmounth, cardcvv, destinationcard, cardamount;
    @FXML private TextFlow sourcecard1;
    @FXML private Label sourcecardshow, cardyearshow, cardmonthshow, cardcvvshow;
    @FXML private Label cardownername, carddestinationbank, cardtransferamount, cardtimer;
    @FXML private AnchorPane tocardotp, cardrecip;
    @FXML private PasswordField cardgetotp;
    @FXML private Button comfirmcard, cardcomfirm, cardsendotp, cardtransfer, printcard, backtocard, backtocard1;
    @FXML private Label sourcenamerec, sourcebankrec, destnamerec, destbankrec, cardtractionrec, cardtime, cardamountrec, cardstatusrec;
    // account to account
    @FXML private TextField sourceaccount, destinationaccount, accounttransferamount;
    @FXML private TextFlow sourceaccount1;
    @FXML private Label sourceaccountshow, destinationaccountshow, sourceaccountnameshow, destinationaccountnameshow, timeraccount;
    @FXML private PasswordField getotpaccount;
    @FXML private Button sendotpaccount, accountprint, backtoaccount;
    @FXML private AnchorPane account1, account2, accountrecip;
    @FXML private Label accountstatus, accountsourcenamerec, accountbankrec, accountdestinationnamerec, accountdestinationbankrec,
            accountmoneyrec, accounttractionid, accounttime;
    // IBAN
    @FXML private TextField sourceiban, ibanyear, ibanmonth, destinationiban, ibantransferamount;
    @FXML private TextFlow sourceiban1;
    @FXML private PasswordField ibancvv;
    @FXML private Button paya, satna, instant, ibanconfirm, ibandone, ibanback;
    @FXML private Label ibansourcename, ibansourcebank, ibandestname, ibandestbank, ibanamountrec, ibantraceno, ibantimerec;
    // back to the home screen
    @FXML private Button backtomain, backtomain1, backtomain3;

    private final Map<String, AnchorPane> sections = new LinkedHashMap<>();
    private OtpChallenge cardOtp;
    private OtpChallenge accountOtp;
    private Recipient cardRecipient;
    private Money cardMoney;
    private IbanMethod ibanMethod;
    private double dragStartX, dragStartY;
    private boolean overTarget;

    @Override
    protected void onLoad() {
        sections.put("cardtocard", cardtocard);
        sections.put("cardotp", cardotp);
        sections.put("cardrecipe", cardrecipe);
        sections.put("accounttransfer", accounttransfer);
        sections.put("accountrecipe", accountrecipe);
        sections.put("ibantransfer", ibantransfer);
        sections.put("ibanrecipe", ibanrecipe);
        showSection("cardtocard");
        homeButtons(backtomain, backtomain1, backtomain3);
        Account mine = Dialogs.attempt(() -> app().customers().primaryAccount(me()).orElse(null));
        setUpCardForm(mine);
        setUpAccountForm(mine);
        setUpIbanForm(mine);
    }

    @Override
    public void showSection(String section) {
        sections.forEach((name, pane) -> pane.setVisible(name.equals(section)));
    }

    /* ================= card to card ================= */

    private void setUpCardForm(Account mine) {
        Inputs.digits(sourcecard, 16, sourcecardshow);
        Inputs.digits(cardyear, 4, cardyearshow);
        Inputs.digits(cardmounth, 2, cardmonthshow);
        Inputs.digits(cardcvv, 4, cardcvvshow);
        Inputs.digits(destinationcard, 16);
        Inputs.digits(cardamount, 15); // digits only: the old field accepted "-1000000"
        SuggestionField.attach(sourcecard, sourcecard1, mine == null ? null : mine.cardNumber(),
                () -> fillExpiry(mine, cardyear, cardmounth), () -> { });
        comfirmcard.setOnAction(e -> reviewCardTransfer());
        cardcomfirm.setOnAction(e -> tocardotp.setVisible(!tocardotp.isVisible()));
        cardOtp = new OtpChallenge(cardsendotp, cardtimer);
        cardtransfer.setOnAction(e -> sendCardTransfer());
        printcard.setOnAction(e -> ReceiptPrinter.saveAsPdf(cardrecip));
        backtocard.setOnAction(e -> showSection("cardtocard"));
        backtocard1.setOnAction(e -> showSection("cardtocard"));
    }

    private void reviewCardTransfer() {
        Dialogs.attempt(() -> {
            app().transfers().ownCard(me(), sourcecard.getText(), cardcvv.getText(), expiry(cardyear, cardmounth));
            cardMoney = Money.parse(cardamount.getText());
            if (!cardMoney.isPositive()) throw new IllegalArgumentException("Enter an amount greater than zero.");
            cardRecipient = app().transfers().recipientByCard(destinationcard.getText());
            cardownername.setText(cardRecipient.name());
            carddestinationbank.setText(cardRecipient.bank());
            cardtransferamount.setText(cardMoney.format() + " IRR  (+" + TransferService.CARD_FEE.format() + " fee)");
            tocardotp.setVisible(false);
            showSection("cardotp");
        });
    }

    private void sendCardTransfer() {
        if (!cardOtp.accept(cardgetotp.getText())) {
            Dialogs.error("The one-time code is wrong or has expired.");
            return;
        }
        try {
            LedgerEntry done = app().transfers().cardToCard(me(), sourcecard.getText(), cardcvv.getText(), expiry(cardyear, cardmounth),
                    destinationcard.getText(), cardMoney);
            showCardReceipt(done, true);
        } catch (InsufficientFundsException e) {
            showCardReceipt(null, false);
        } catch (RuntimeException e) {
            Dialogs.attempt(() -> { throw e; });
            return;
        }
        cardOtp.reset();
        for (TextField f : new TextField[]{cardgetotp, sourcecard, cardyear, cardmounth, cardcvv, destinationcard, cardamount}) f.clear();
        showSection("cardrecipe");
    }

    private void showCardReceipt(LedgerEntry e, boolean ok) {
        sourcenamerec.setText(e != null ? e.sourceName() : Dialogs.attempt(() -> app().customers().primaryAccount(me()).map(Account::fullName).orElse("")));
        sourcebankrec.setText(BankDirectory.OWN_BANK);
        destnamerec.setText(cardRecipient.name());
        destbankrec.setText(cardRecipient.bank());
        cardtractionrec.setText(e != null ? String.valueOf(e.id()) : "-");
        cardtime.setText((e != null ? e.time() : java.time.LocalDateTime.now()).format(WHEN));
        cardamountrec.setText(cardMoney.format());
        cardstatusrec.setText(ok ? "SUCCESSFUL" : "UNSUCCESSFUL: not enough money");
        cardstatusrec.setStyle(ok ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
    }

    /* ================= account to account ================= */

    private void setUpAccountForm(Account mine) {
        Inputs.digits(sourceaccount, 10, sourceaccountshow);
        Inputs.digits(destinationaccount, 10, destinationaccountshow);
        Inputs.digits(accounttransferamount, 15);
        Inputs.digits(getotpaccount, 5);
        SuggestionField.attach(sourceaccount, sourceaccount1, mine == null ? null : mine.accountNumber(), () -> { }, () -> { });
        sourceaccount.textProperty().addListener((o, a, now) -> sourceaccountnameshow.setText(now.length() == 10 ? ownAccountName(now) : ""));
        destinationaccount.textProperty().addListener((o, a, now) -> destinationaccountnameshow.setText(now.length() == 10 ? recipientName(now) : ""));
        accountOtp = new OtpChallenge(sendotpaccount, timeraccount, () -> {
            if (!sourceaccountnameshow.getText().isEmpty()) return true;
            Dialogs.error("Enter one of your own account numbers first.");
            return false;
        });
        getotpaccount.textProperty().addListener((o, a, now) -> {
            account1.setDisable(now.length() != 5);
            account2.setDisable(now.length() != 5);
        });
        account1.setOnMousePressed(this::dragStart);
        account1.setOnMouseDragged(this::drag);
        account1.setOnMouseReleased(this::drop);
        account1.setDisable(true);
        account2.setDisable(true);
        accountprint.setOnAction(e -> ReceiptPrinter.saveAsPdf(accountrecip));
        backtoaccount.setOnAction(e -> showSection("accounttransfer"));
    }

    private String ownAccountName(String number) {
        try {
            return app().transfers().ownAccount(me(), number).fullName();
        } catch (RuntimeException e) {
            return "";
        }
    }

    private String recipientName(String number) {
        try {
            return app().transfers().recipientByAccount(number).name();
        } catch (RuntimeException e) {
            return "Unknown";
        }
    }

    private void dragStart(MouseEvent e) {
        dragStartX = account1.getLayoutX();
        dragStartY = account1.getLayoutY();
    }

    /** The target gets a thicker outline the closer the dragged account is; yellow means "drop here". */
    private void drag(MouseEvent e) {
        account1.toFront();
        account1.setLayoutX(e.getSceneX() - account1.getWidth() / 2);
        account1.setLayoutY(e.getSceneY() - account1.getHeight() / 2);
        double dx = (account1.getLayoutX() + account1.getWidth() / 2) - (account2.getLayoutX() + account2.getWidth() / 2);
        double dy = (account1.getLayoutY() + account1.getHeight() / 2) - (account2.getLayoutY() + account2.getHeight() / 2);
        double distance = Math.hypot(dx, dy);
        overTarget = distance <= 20;
        if (overTarget) {
            account2.setStyle("-fx-border-color: yellow; -fx-border-width: 4; -fx-border-radius: 20px;");
        } else if (distance <= 150) {
            int thickness = (int) Math.max(1, 4 - ((distance - 20) / (130 / 3.0)));
            account2.setStyle("-fx-border-color: white; -fx-border-width: " + thickness + "; -fx-border-radius: 20px;");
        } else {
            account2.setStyle("");
        }
    }

    private void drop(MouseEvent e) {
        account2.setStyle("");
        account1.setLayoutX(dragStartX);
        account1.setLayoutY(dragStartY);
        if (!overTarget) return;
        overTarget = false;
        if (!accountOtp.accept(getotpaccount.getText())) {
            Dialogs.error("The one-time code is wrong or has expired.");
            return;
        }
        LedgerEntry done = Dialogs.attempt(() -> app().transfers().accountToAccount(me(), sourceaccount.getText(),
                destinationaccount.getText(), Money.parse(accounttransferamount.getText())));
        if (done == null) return;
        accountstatus.setText("SUCCESSFUL");
        accountstatus.setStyle("-fx-text-fill: green;");
        accountsourcenamerec.setText(done.sourceName());
        accountbankrec.setText(BankDirectory.OWN_BANK);
        accountdestinationnamerec.setText(done.destinationName());
        accountdestinationbankrec.setText(done.destinationBank());
        accountmoneyrec.setText(done.amount().format());
        accounttractionid.setText(String.valueOf(done.id()));
        accounttime.setText(done.time().format(WHEN));
        accountOtp.reset();
        for (TextField f : new TextField[]{sourceaccount, destinationaccount, accounttransferamount, getotpaccount}) f.clear();
        showSection("accountrecipe");
    }

    /* ================= IBAN ================= */

    private void setUpIbanForm(Account mine) {
        Inputs.digits(sourceiban, 26);
        Inputs.digits(destinationiban, 26);
        Inputs.digits(ibantransferamount, 15);
        Inputs.digits(ibanyear, 4);
        Inputs.digits(ibanmonth, 2);
        Inputs.digits(ibancvv, 4);
        String myIban = mine == null || mine.iban() == null ? null : mine.iban().replaceFirst("^IR", "");
        SuggestionField.attach(sourceiban, sourceiban1, myIban, () -> fillExpiry(mine, ibanyear, ibanmonth), () -> { });
        paya.setOnAction(e -> chooseMethod(IbanMethod.PAYA));
        satna.setOnAction(e -> chooseMethod(IbanMethod.SATNA));
        instant.setOnAction(e -> chooseMethod(IbanMethod.INSTANT));
        ibanconfirm.setOnAction(e -> sendIbanTransfer());
        ibandone.setOnAction(e -> showSection("ibantransfer"));
        ibanback.setOnAction(e -> showSection("ibantransfer"));
    }

    private void chooseMethod(IbanMethod method) {
        ibanMethod = method;
        paya.setDisable(method == IbanMethod.PAYA);
        satna.setDisable(method == IbanMethod.SATNA);
        instant.setDisable(method == IbanMethod.INSTANT);
    }

    private void sendIbanTransfer() {
        if (ibanMethod == null) {
            Dialogs.error("Choose Paya, Satna or Instant first.");
            return;
        }
        LedgerEntry done = Dialogs.attempt(() -> app().transfers().ibanTransfer(me(), sourceiban.getText(), ibancvv.getText(),
                expiry(ibanyear, ibanmonth), destinationiban.getText(), Money.parse(ibantransferamount.getText()), ibanMethod));
        if (done == null) return;
        // the receipt was designed but never filled in before
        ibansourcename.setText(done.sourceName());
        ibansourcebank.setText(BankDirectory.OWN_BANK);
        ibandestname.setText(done.destinationName());
        ibandestbank.setText(done.destinationBank());
        ibanamountrec.setText(done.amount().format() + " IRR");
        ibantraceno.setText(String.valueOf(done.id()));
        ibantimerec.setText(done.time().format(WHEN));
        for (TextField f : new TextField[]{sourceiban, ibanyear, ibanmonth, ibancvv, destinationiban, ibantransferamount}) f.clear();
        showSection("ibanrecipe");
    }

    /* ================= shared ================= */

    /** Expiry dates are stored as "year.month". */
    private static String expiry(TextField year, TextField month) {
        return year.getText().strip() + "." + month.getText().strip();
    }

    private static void fillExpiry(Account mine, TextField year, TextField month) {
        if (mine == null || mine.expiryDate() == null) return;
        String[] parts = mine.expiryDate().split("\\.");
        if (parts.length == 2) {
            year.setText(parts[0]);
            month.setText(parts[1]);
        }
    }
}
