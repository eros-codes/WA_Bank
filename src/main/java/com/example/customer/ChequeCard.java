package com.example.customer;

import com.example.bank.model.Cheque;
import com.example.bank.ui.Card;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.time.format.DateTimeFormatter;

/** One written cheque, with its review status. */
public class ChequeCard implements Card<Cheque> {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    @FXML private Label issueadd;
    @FXML private TextField numberadd, amountadd, recipientadd, dueadd, dateadd;

    @Override
    public void show(Cheque c) {
        issueadd.setText(c.issuerName() + "  ·  " + c.status().label());
        numberadd.setText(String.valueOf(c.number()));
        amountadd.setText(c.amount().format());
        recipientadd.setText(c.recipientName());
        dueadd.setText(c.dueDate() == null ? "" : c.dueDate().format(DAY));
        dateadd.setText(c.date() == null ? "" : c.date().format(DAY));
        for (TextField f : new TextField[]{numberadd, amountadd, recipientadd, dueadd, dateadd}) f.setEditable(false);
    }
}
