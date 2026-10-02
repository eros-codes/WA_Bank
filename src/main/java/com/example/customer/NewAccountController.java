package com.example.customer;

import com.example.bank.model.Account;
import com.example.bank.model.AccountType;
import com.example.bank.model.Money;
import com.example.bank.service.CustomerService;
import com.example.bank.ui.Dialogs;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Opening another account. It gets its own new numbers and waits for review, like the first one. */
public class NewAccountController extends CustomerPage {

    @FXML private Button checking, saving, add, back1;
    @FXML private TextField deposit;
    @FXML private CheckBox accepted;
    @FXML private Canvas sign;
    @FXML private Label daate;

    private AccountType type;
    private SignaturePad signature;

    @Override
    protected void onLoad() {
        back1.setOnAction(e -> open(back1, CustomerScreen.ACCOUNTS));
        daate.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")));
        Inputs.digits(deposit, 15);
        checking.setOnAction(e -> choose(AccountType.CHECKING, checking, saving));
        saving.setOnAction(e -> choose(AccountType.SAVING, saving, checking));
        signature = new SignaturePad(sign);
        add.setOnAction(e -> openAccount());
    }

    private void choose(AccountType t, Button chosen, Button other) {
        type = t;
        chosen.setDisable(true);
        other.setDisable(false);
    }

    private void openAccount() {
        if (type == null) { Dialogs.error("Choose an account type."); return; }
        if (!accepted.isSelected()) { Dialogs.error("Please accept the terms."); return; }
        if (!signature.isSigned()) { Dialogs.error("Please sign in the box."); return; }
        if (deposit.getText().isBlank() || Money.parse(deposit.getText()).compareTo(CustomerService.MINIMUM_OPENING) < 0) {
            Dialogs.error("The first deposit must be at least " + CustomerService.MINIMUM_OPENING.format() + " IRR.");
            return;
        }
        Account created = Dialogs.attempt(() -> app().customers().openAnotherAccount(me(), type, Money.parse(deposit.getText())));
        if (created == null) return;
        Dialogs.info("Account requested", "Your new account " + created.accountNumber() + " will be usable once a bank employee approves it.");
        open(add, CustomerScreen.ACCOUNTS);
    }
}
