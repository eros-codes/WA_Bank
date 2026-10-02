package com.example.staff;

import com.example.bank.model.Loan;
import com.example.bank.model.LoanStatus;
import com.example.bank.ui.Card;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/** A loan that has moved on, with its status in colour. */
public class DecidedLoanCard implements Card<Loan> {

    @FXML private Label loanfullname, loanamount, loanusername, loantype, loandate, loanfinancial, loanstatus;

    @Override
    public void show(Loan loan) {
        loanfullname.setText(loan.applicantName());
        loanusername.setText(loan.username());
        loanamount.setText(loan.amount().format());
        loantype.setText(loan.term());
        loandate.setText(loan.date());
        loanfinancial.setText(loan.interest());
        loanstatus.setText(loan.status().label());
        loanstatus.setStyle(loan.status() == LoanStatus.DENIED ? "-fx-text-fill: red;"
                : loan.status() == LoanStatus.ACCEPTED ? "-fx-text-fill: green;" : "-fx-text-fill: black;");
    }
}
