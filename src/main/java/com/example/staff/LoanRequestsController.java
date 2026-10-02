package com.example.staff;

import com.example.bank.model.Loan;
import com.example.bank.ui.Dialogs;
import com.example.bank.ui.Navigator;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.TilePane;

import java.util.List;

/**
 * Loan requests. What a role sees and what its decision does comes from the
 * role: an employee forwards or denies, the manager accepts (paying the loan
 * out) or denies. A loan is identified by its id, never by owner and amount.
 */
public class LoanRequestsController extends StaffPage {

    private static final String REQUESTS = "Loans";
    private static final String HISTORY = "Loan Status";

    @FXML private AnchorPane loanhome, loandetails;
    @FXML private Label loanpendings, loanapproved, loandeclined;
    @FXML private ComboBox<String> filter;
    @FXML private TilePane tilee;
    @FXML private Button loandetail, back;

    public LoanRequestsController(StaffRole role) {
        super(role);
    }

    @Override
    protected boolean backGoesToDashboard() {
        return false; // here "back" closes the details panel
    }

    @Override
    protected void onLoad() {
        loanhome.setVisible(true);
        loandetails.setVisible(false);
        loandetail.setOnAction(e -> loandetails.setVisible(true));
        back.setOnAction(e -> loandetails.setVisible(false));
        filter.getItems().setAll(REQUESTS, HISTORY);
        filter.setValue(REQUESTS);
        filter.valueProperty().addListener((o, a, b) -> refresh());
        refresh();
    }

    private void refresh() {
        Dialogs.attempt(() -> {
            var loans = app().loans();
            loanpendings.setText(String.valueOf(loans.count(role.loansInProgress())));
            loanapproved.setText(String.valueOf(loans.count(java.util.EnumSet.of(com.example.bank.model.LoanStatus.ACCEPTED))));
            loandeclined.setText(String.valueOf(loans.count(java.util.EnumSet.of(com.example.bank.model.LoanStatus.DENIED))));
            if (REQUESTS.equals(filter.getValue())) {
                List<Loan> open = loans.list(role.loansToDecide());
                Navigator.<Loan, LoanCard>fill(tilee, open, role.resource(Page.LOAN_CARD), (card, loan) -> card.show(loan, approve -> decide(loan, approve)));
            } else {
                Navigator.fillCards(tilee, loans.list(role.loanHistory()), role.resource(Page.DECIDED_LOAN_CARD));
            }
            Tiles.margin(tilee, 10);
        });
    }

    private void decide(Loan loan, boolean approve) {
        if (Dialogs.attempt(() -> role.decideLoan(app().loans(), loan.id(), approve))) refresh();
    }
}
