package com.example.staff;

import com.example.bank.model.BillPayment;
import com.example.bank.ui.Card;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class BillCard implements Card<BillPayment> {

    @FXML private Label billtype, username, amount, paymentid, fullname;

    @Override
    public void show(BillPayment b) {
        billtype.setText(b.billType());
        username.setText(b.username());
        amount.setText(b.amount().format());
        paymentid.setText(String.valueOf(b.billId()));
        fullname.setText(b.paidBy());
    }
}
