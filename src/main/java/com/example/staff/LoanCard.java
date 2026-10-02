package com.example.staff;

import com.example.bank.model.Loan;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.util.function.Consumer;

/** A loan request with its approve and decline buttons. */
public class LoanCard {

    @FXML private Label fullnameloan, usernameloan, amountloan, typeloan, dateloan, financialloan;
    @FXML private Button approved, declined;

    public void show(Loan loan, Consumer<Boolean> decide) {
        fullnameloan.setText(loan.applicantName());
        usernameloan.setText(loan.username());
        amountloan.setText(loan.amount().format());
        typeloan.setText(loan.term());
        dateloan.setText(loan.date());
        financialloan.setText(loan.interest());
        approved.setOnAction(e -> decide.accept(true));
        declined.setOnAction(e -> decide.accept(false));
    }
}
