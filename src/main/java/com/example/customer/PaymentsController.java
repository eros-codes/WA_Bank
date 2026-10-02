package com.example.customer;

import com.example.bank.model.Account;
import com.example.bank.model.BillPayment;
import com.example.bank.model.BillType;
import com.example.bank.model.MobileOperator;
import com.example.bank.model.Money;
import com.example.bank.model.TopUp;
import com.example.bank.service.InsufficientFundsException;
import com.example.bank.service.PaymentService;
import com.example.bank.ui.Dialogs;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.TextFlow;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/** Paying a bill, or buying phone credit, with a card. The payment itself happens in PaymentService. */
public class PaymentsController extends CustomerPage implements Sectioned {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private AnchorPane billpage, billid, billpayment, billrecipe, billrecip, charge, chargepayment, chargerecipe, chargerecip;
    // bills
    @FXML private Button wbill, ebill, gbill, tbill, ibill, paythebill, billsendotp, paymentdone, brecprint;
    @FXML private Button backtomain, backtobill, backtobillid, backtobills1;
    @FXML private Label billtype, billamount, amountbill, billtimer, notacceptedbill, sendcodebill;
    @FXML private TextField billidnum, paymentidnum, dueyear, billcard, billyear, billmonth;
    @FXML private PasswordField billcvv, billgetotp;
    @FXML private TextFlow billcard1;
    @FXML private Label brecamount, brecbid, brecpid, brecduedate, brectype, brecdate, payed, not;
    // top-ups
    @FXML private TextField phonenum, chargecard, chargeyear, chargemonth;
    @FXML private PasswordField chargecvv, chargeotp;
    @FXML private TextFlow chargecard1;
    @FXML private ImageView mci, rightel, irancell;
    @FXML private Button r5, r10, r20, r50, chargepeyment, chargesendotp, chargepay, chrecprint, backtocharge, backtobills, backtobillcharge;
    @FXML private Label operattor, chargeamount, chargetimer, notaccepted, sendcode;
    @FXML private Label chrecoperator, chrecphnum, chrecamount, chrecid, chargestatus, chargedate;

    private final Map<String, AnchorPane> sections = new LinkedHashMap<>();
    private BillType bill;
    private int billNumber;
    private Money credit;
    private Account payingCard;
    private OtpChallenge billOtp;
    private OtpChallenge chargeOtp;

    @Override
    protected void onLoad() {
        sections.put("billpage", billpage);
        sections.put("billid", billid);
        sections.put("billpayment", billpayment);
        sections.put("billrecipe", billrecipe);
        sections.put("charge", charge);
        sections.put("chargepayment", chargepayment);
        sections.put("chargerecipe", chargerecipe);
        showSection("billpage");
        homeButtons(backtomain, backtobillcharge);
        Account mine = Dialogs.attempt(() -> app().customers().primaryAccount(me()).orElse(null));
        setUpBills(mine);
        setUpTopUps(mine);
    }

    @Override
    public void showSection(String section) {
        sections.forEach((name, pane) -> pane.setVisible(name.equals(section)));
    }

    /* ================= bills ================= */

    private void setUpBills(Account mine) {
        wbill.setOnAction(e -> chooseBill(BillType.WATER));
        ebill.setOnAction(e -> chooseBill(BillType.ELECTRIC));
        gbill.setOnAction(e -> chooseBill(BillType.GAS));
        tbill.setOnAction(e -> chooseBill(BillType.TELEPHONE));
        ibill.setOnAction(e -> chooseBill(BillType.INTERNET));
        paythebill.setOnAction(e -> {
            amountbill.setText(withTax(bill.amount()));
            showSection("billpayment");
        });
        Inputs.digits(billcard, 16);
        Inputs.digits(billyear, 4);
        Inputs.digits(billmonth, 2);
        Inputs.digits(billcvv, 4);
        SuggestionField.attach(billcard, billcard1, mine == null ? null : mine.cardNumber(), () -> fillExpiry(mine, billyear, billmonth), () -> { });
        billOtp = new OtpChallenge(billsendotp, billtimer, () -> checkCard(billcard, billcvv, billyear, billmonth, notacceptedbill, sendcodebill));
        paymentdone.setOnAction(e -> payBill());
        brecprint.setOnAction(e -> ReceiptPrinter.saveAsPdf(billrecip));
        backtobill.setOnAction(e -> showSection("billpage"));
        backtobillid.setOnAction(e -> showSection("billid"));
        backtobills1.setOnAction(e -> showSection("billpage"));
    }

    private void chooseBill(BillType type) {
        Integer id = Dialogs.attempt(() -> app().payments().newBillId());
        if (id == null) return;
        bill = type;
        billNumber = id;
        billtype.setText(type.label());
        billamount.setText(type.amount().format() + " IRR");
        billidnum.setText(String.valueOf(id));
        paymentidnum.setText("given when paid");
        dueyear.setText(YearMonth.now().atEndOfMonth().format(DAY));
        showSection("billid");
    }

    private void payBill() {
        if (!billOtp.accept(billgetotp.getText())) {
            Dialogs.error("The one-time code is wrong or has expired.");
            return;
        }
        brectype.setText(bill.label());
        brecamount.setText(bill.amount().format() + " IRR");
        brecbid.setText(String.valueOf(billNumber));
        brecduedate.setText(dueyear.getText());
        try {
            BillPayment paid = app().payments().payBill(payingCard, bill, billNumber, dueyear.getText());
            brecpid.setText(String.valueOf(paid.transactionNo()));
            brecdate.setText(paid.paidAt().format(WHEN));
            payed.setVisible(true);
            not.setVisible(false);
        } catch (InsufficientFundsException e) {
            Dialogs.error("You don't have enough money for this payment.");
            brecpid.setText("-");
            brecdate.setText(LocalDate.now().format(DAY));
            payed.setVisible(false);
            not.setVisible(true);
        } catch (RuntimeException e) {
            Dialogs.attempt(() -> { throw e; });
            return;
        }
        billOtp.reset();
        for (TextField f : new TextField[]{billcard, billyear, billmonth, billcvv, billgetotp}) f.clear();
        showSection("billrecipe");
    }

    /* ================= top-ups ================= */

    private void setUpTopUps(Account mine) {
        mci.setVisible(false);
        rightel.setVisible(false);
        irancell.setVisible(false);
        Inputs.digits(phonenum, 11);
        phonenum.textProperty().addListener((o, a, now) -> showOperator(now));
        Map<Button, Long> amounts = Map.of(r5, 50_000L, r10, 100_000L, r20, 200_000L, r50, 500_000L);
        amounts.forEach((button, value) -> button.setOnAction(e -> {
            credit = Money.of(value);
            chargeamount.setText(credit.format() + " IRR");
            amounts.keySet().forEach(b -> {
                b.getStyleClass().remove("menubtnselected");
                if (!b.getStyleClass().contains("menubtn")) b.getStyleClass().add("menubtn");
            });
            button.getStyleClass().remove("menubtn");
            button.getStyleClass().add("menubtnselected");
        }));
        chargepeyment.setOnAction(e -> {
            if (credit == null) Dialogs.error("Choose an amount of credit.");
            else if (MobileOperator.of(phonenum.getText()).isEmpty() || phonenum.getText().length() != 11) Dialogs.error("Enter a valid mobile number, like 09123456789.");
            else showSection("chargepayment");
        });
        Inputs.digits(chargecard, 16);
        Inputs.digits(chargeyear, 4);
        Inputs.digits(chargemonth, 2);
        Inputs.digits(chargecvv, 4);
        SuggestionField.attach(chargecard, chargecard1, mine == null ? null : mine.cardNumber(), () -> fillExpiry(mine, chargeyear, chargemonth), () -> { });
        chargeOtp = new OtpChallenge(chargesendotp, chargetimer, () -> checkCard(chargecard, chargecvv, chargeyear, chargemonth, notaccepted, sendcode));
        chargepay.setOnAction(e -> buyCredit());
        chrecprint.setOnAction(e -> ReceiptPrinter.saveAsPdf(chargerecip));
        backtocharge.setOnAction(e -> showSection("charge"));
        backtobills.setOnAction(e -> showSection("billpage"));
    }

    private void showOperator(String phone) {
        var op = MobileOperator.of(phone);
        operattor.setText(op.map(MobileOperator::label).orElse(phone.length() >= 4 ? "your phone number is not valid" : ""));
        mci.setVisible(op.orElse(null) == MobileOperator.HAMRAH_AVAL);
        rightel.setVisible(op.orElse(null) == MobileOperator.RIGHTEL);
        irancell.setVisible(op.orElse(null) == MobileOperator.IRANCELL);
    }

    private void buyCredit() {
        if (!chargeOtp.accept(chargeotp.getText())) {
            Dialogs.error("The one-time code is wrong or has expired.");
            return;
        }
        chrecoperator.setText(operattor.getText());
        chrecphnum.setText(phonenum.getText());
        chrecamount.setText(credit.format() + " IRR");
        try {
            TopUp done = app().payments().topUp(payingCard, phonenum.getText(), credit);
            chrecid.setText(String.valueOf(done.transactionNo()));
            chargedate.setText(done.time().format(WHEN));
            chargestatus.setText("Successful");
            chargestatus.setStyle("-fx-text-fill: green;");
        } catch (InsufficientFundsException e) {
            Dialogs.error("You don't have enough money for this operation.");
            chrecid.setText("-");
            chargedate.setText(LocalDate.now().format(DAY));
            chargestatus.setText("Unsuccessful");
            chargestatus.setStyle("-fx-text-fill: red;");
        } catch (RuntimeException e) {
            Dialogs.attempt(() -> { throw e; });
            return;
        }
        chargeOtp.reset();
        for (TextField f : new TextField[]{chargecard, chargeyear, chargemonth, chargecvv, chargeotp}) f.clear();
        showSection("chargerecipe");
    }

    /* ================= shared ================= */

    /** Before a code is sent, the card must be right: the screen says so in place, as it was designed to. */
    private boolean checkCard(TextField card, TextField cvv, TextField year, TextField month, Node rejected, Node sent) {
        try {
            payingCard = app().payments().cardFor(card.getText(), cvv.getText(), year.getText().strip() + "." + month.getText().strip());
            rejected.setVisible(false);
            sent.setVisible(true);
            return true;
        } catch (RuntimeException e) {
            rejected.setVisible(true);
            sent.setVisible(false);
            return false;
        }
    }

    private static String withTax(Money amount) {
        return amount.plus(PaymentService.tax(amount)).format() + " IRR (with 9% tax)";
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
