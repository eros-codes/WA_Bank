package com.example.staff;

import com.example.bank.model.TermDeposit;
import com.example.bank.ui.Card;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/** A fixed-term deposit. The name field used to show the username. */
public class DepositCard implements Card<TermDeposit> {

    @FXML private Label fullname, accountnum, depositduration, deposittype, depositamount, depositpayment, depositinterest;

    @Override
    public void show(TermDeposit d) {
        fullname.setText(d.ownerName() != null ? d.ownerName() : d.username());
        accountnum.setText(String.valueOf(d.number()));
        deposittype.setText(d.type());
        depositamount.setText(d.amount().format());
        depositduration.setText(d.duration());
        depositinterest.setText(d.interest());
        depositpayment.setText(d.payment());
    }
}
