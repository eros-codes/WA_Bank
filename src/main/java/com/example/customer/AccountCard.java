package com.example.customer;

import com.example.bank.model.Account;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class AccountCard {

    @FXML private Label accnum, acctype, accamount;
    @FXML private Button seedetail;

    public void show(Account a, Runnable onDetails) {
        accnum.setText(a.accountNumber());
        acctype.setText(a.type().dbValue());
        accamount.setText(a.balance().format() + " IRR");
        seedetail.setOnAction(e -> onDetails.run());
    }
}
