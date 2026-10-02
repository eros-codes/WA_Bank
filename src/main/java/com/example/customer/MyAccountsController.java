package com.example.customer;

import com.example.bank.model.Account;
import com.example.bank.ui.Dialogs;
import com.example.bank.ui.Navigator;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

import java.util.List;

/** The customer's accounts, one card's details at a time, and closing an empty account. */
public class MyAccountsController extends CustomerPage {

    @FXML private AnchorPane acc, detail;
    @FXML private VBox seeacc;
    @FXML private Label accnumber, accamount, acctype, cardnum, cvv2, expired, iban;
    @FXML private CheckBox accept;
    @FXML private Button back, add, closeaccount, backtoacc;

    private Account shown;

    @Override
    protected void onLoad() {
        homeButtons(back);
        add.setOnAction(e -> open(add, CustomerScreen.NEW_ACCOUNT));
        backtoacc.setOnAction(e -> showList());
        closeaccount.setOnAction(e -> close());
        showList();
    }

    private void showList() {
        detail.setVisible(false);
        acc.setVisible(true);
        List<Account> mine = Dialogs.attempt(() -> app().customers().accountsOf(me()));
        if (mine == null) return;
        Navigator.<Account, AccountCard>fill(seeacc, mine, CustomerScreen.ACCOUNT_CARD.resource(), (card, a) -> card.show(a, () -> details(a)));
        seeacc.getChildren().forEach(n -> VBox.setMargin(n, new Insets(10)));
    }

    private void details(Account a) {
        shown = a;
        accnumber.setText(a.accountNumber());
        accamount.setText(a.balance().format() + " IRR");
        acctype.setText(a.type().dbValue() + (a.isUsable() ? "" : " (waiting for approval)"));
        cardnum.setText(a.formattedCardNumber());
        cvv2.setText(a.cvv());
        expired.setText(a.expiryDate());
        iban.setText(a.iban());
        accept.setSelected(false);
        acc.setVisible(false);
        detail.setVisible(true);
    }

    /** The old screen deleted the account with its money; an account must be empty to close. */
    private void close() {
        if (!accept.isSelected()) {
            Dialogs.error("Please accept the terms first.");
            return;
        }
        if (shown == null || !Dialogs.confirm("Close account " + shown.accountNumber() + "?")) return;
        if (Dialogs.attempt(() -> app().customers().closeAccount(me(), shown.accountNumber()))) {
            Dialogs.info("Closed", "The account has been closed.");
            showList();
        }
    }
}
