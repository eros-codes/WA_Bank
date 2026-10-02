package com.example.customer;

import com.example.bank.model.TermDeposit;
import com.example.bank.ui.Dialogs;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;

/** One deposit: its terms, what each payout is, and closing it. */
public class DepositDetailsController extends CustomerPage {

    @FXML private Label deponumber, depotype, depoamount, depoduration, depointerest, depowithdraw;
    @FXML private CheckBox accept;
    @FXML private Button close, back;

    private TermDeposit deposit;

    @Override
    protected void onLoad() {
        back.setOnAction(e -> open(back, CustomerScreen.MY_DEPOSITS));
        close.setOnAction(e -> closeDeposit());
    }

    /** Called by the list with the deposit to show (the old screens passed it through a static field). */
    public void load(int number) {
        deposit = Dialogs.attempt(() -> app().deposits().find(me(), number));
        if (deposit == null) return;
        deponumber.setText(String.valueOf(deposit.number()));
        depotype.setText(deposit.type());
        depoamount.setText(deposit.amount().format() + " " + deposit.currency());
        depoduration.setText(deposit.duration());
        depointerest.setText(deposit.interest());
        depowithdraw.setText(deposit.periodicPayout().format() + " " + deposit.currency());
    }

    private void closeDeposit() {
        if (!accept.isSelected()) {
            Dialogs.error("Please accept the terms before closing the deposit.");
            return;
        }
        if (deposit == null || !Dialogs.confirm("Close this deposit and return " + deposit.valueInRials().format() + " IRR to your account?")) return;
        if (Dialogs.attempt(() -> app().deposits().close(me(), deposit.number()))) {
            Dialogs.info("Closed", "The deposit was closed and its value returned to your account.");
            open(close, CustomerScreen.MY_DEPOSITS);
        }
    }
}
