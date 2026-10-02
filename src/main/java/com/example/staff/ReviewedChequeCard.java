package com.example.staff;

import com.example.bank.model.Cheque;
import com.example.bank.model.ChequeStatus;
import com.example.bank.ui.Card;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/** A cheque that has been reviewed, with its status in colour. */
public class ReviewedChequeCard implements Card<Cheque> {

    @FXML private Label checknumber, checkamount, recipentnname, duedate, nowdate, status;

    @Override
    public void show(Cheque c) {
        checknumber.setText(String.valueOf(c.number()));
        checkamount.setText(c.amount().format());
        recipentnname.setText(c.recipientName());
        duedate.setText(c.dueDate() == null ? "" : c.dueDate().format(TransferCard.WHEN));
        nowdate.setText(c.date() == null ? "" : c.date().format(TransferCard.WHEN));
        status.setText(c.status().label());
        status.setStyle(c.status() == ChequeStatus.ACCEPTED ? "-fx-text-fill: green;"
                : c.status() == ChequeStatus.DENIED ? "-fx-text-fill: red;" : "-fx-text-fill: black;");
    }
}
