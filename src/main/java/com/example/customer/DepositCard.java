package com.example.customer;

import com.example.bank.model.TermDeposit;
import com.example.bank.ui.Card;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

/** One deposit in the list; its button opens the details. */
public class DepositCard implements Card<TermDeposit> {

    @FXML private Label deponum, depoamount, depotype;
    @FXML private Button depodetail;

    @Override
    public void show(TermDeposit d) {
        deponum.setText(String.valueOf(d.number()));
        depoamount.setText(d.amount().format() + " " + d.currency());
        depotype.setText(d.type());
        depodetail.setOnAction(e -> CustomerPage.<DepositDetailsController>open(depodetail, CustomerScreen.DEPOSIT_DETAILS, c -> c.load(d.number())));
    }
}
