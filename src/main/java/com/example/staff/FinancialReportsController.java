package com.example.staff;

import com.example.bank.ui.Dialogs;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/** The numbers: transfers, bills, loan requests and the bank's own funds. */
public class FinancialReportsController extends StaffPage {

    @FXML private Label tansaction, bills, request, entered;

    public FinancialReportsController(StaffRole role) {
        super(role);
    }

    @Override
    protected void onLoad() {
        Dialogs.attempt(() -> {
            var reports = app().reports();
            tansaction.setText(String.valueOf(reports.transferCount()));
            bills.setText(String.valueOf(reports.billCount()));
            request.setText(String.valueOf(reports.loanRequestCount()));
            entered.setText(app().staff().bankFunds().format());
        });
    }
}
