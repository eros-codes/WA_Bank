package com.example.staff;

import com.example.bank.model.LedgerEntry;
import com.example.bank.ui.Card;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.time.format.DateTimeFormatter;

/** One transfer: who sent it, from which card, account or IBAN, and where to. */
public class TransferCard implements Card<LedgerEntry> {

    static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private Label sendername, reciptype, sourcenumber, destinationnumber, amount, transactionnum, transactiondate;

    @Override
    public void show(LedgerEntry e) {
        sendername.setText(e.sourceName());
        reciptype.setText(e.kind());
        boolean byAccount = "account".equalsIgnoreCase(e.kind());
        boolean byIban = "iban".equalsIgnoreCase(e.kind());
        sourcenumber.setText(byAccount ? e.sourceAccount() : byIban ? e.sourceIban() : e.sourceCard());
        destinationnumber.setText(byAccount ? e.destinationAccount() : byIban ? e.destinationIban() : e.destinationCard());
        amount.setText(e.amount().format());
        transactionnum.setText(String.valueOf(e.id()));
        transactiondate.setText(e.time() == null ? "" : e.time().format(WHEN));
    }
}
