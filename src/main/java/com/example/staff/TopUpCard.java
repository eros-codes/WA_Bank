package com.example.staff;

import com.example.bank.model.TopUp;
import com.example.bank.ui.Card;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/** A mobile top-up. The name field used to show the account number. */
public class TopUpCard implements Card<TopUp> {

    @FXML private Label username, phonenumber, chargeamount, paymentid, fullname;

    @Override
    public void show(TopUp t) {
        username.setText(t.username());
        phonenumber.setText(t.phoneNumber());
        chargeamount.setText(t.amount().format());
        paymentid.setText(String.valueOf(t.transactionNo()));
        fullname.setText(t.payerName() != null ? t.payerName() : t.accountNo());
    }
}
