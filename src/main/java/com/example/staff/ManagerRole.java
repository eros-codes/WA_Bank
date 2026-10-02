package com.example.staff;

import com.example.bank.model.Employee;
import com.example.bank.model.LoanStatus;
import com.example.bank.service.AuthService;
import com.example.bank.service.LoanService;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** The manager: makes the final decision on loans, manages customers and staff. */
public final class ManagerRole implements StaffRole {

    public static final ManagerRole INSTANCE = new ManagerRole();

    private static final Map<Page, String> FILES = new EnumMap<>(Page.class);

    static {
        FILES.put(Page.LOGIN, "welcomeboss.fxml");
        FILES.put(Page.DASHBOARD, "dashboard M.fxml");
        FILES.put(Page.VERIFY, "verify M.fxml");
        FILES.put(Page.PAYMENTS, "bill payments M.fxml");
        FILES.put(Page.CHANGE_PASSWORD, "change passwordM.fxml");
        FILES.put(Page.REPORTS, "financial reports M.fxml");
        FILES.put(Page.ACCOUNTS, "list of accounts M.fxml");
        FILES.put(Page.DEPOSITS, "desposit m.fxml");
        FILES.put(Page.TRANSACTIONS, "transactions M.fxml");
        FILES.put(Page.LOANS, "loan requests M.fxml");
        FILES.put(Page.LOAN_CARD, "loanhome M.fxml");
        FILES.put(Page.DECIDED_LOAN_CARD, "loanhome2 M.fxml");
        FILES.put(Page.TRANSFER_CARD, "card by card M.fxml");
        FILES.put(Page.CHEQUE_REVIEW_CARD, "check M.fxml");
        FILES.put(Page.REVIEWED_CHEQUE_CARD, "check 1 M.fxml");
        FILES.put(Page.BILL_CARD, "bill M.fxml");
        FILES.put(Page.TOP_UP_CARD, "charge M.fxml");
        FILES.put(Page.DEPOSIT_CARD, "desposit2 M.fxml");
        FILES.put(Page.CUSTOMER_ROW, "listofaccounts2 M.fxml");
        FILES.put(Page.EMPLOYEE_ROW, "employee rights2.fxml");
        FILES.put(Page.MANAGE_CUSTOMER, "manage account M.fxml");
    }

    private ManagerRole() {
    }

    @Override
    public String name() {
        return "Manager";
    }

    @Override
    public String resource(Page page) {
        return "/com/example/boss/" + FILES.get(page);
    }

    @Override
    public Optional<Employee> signIn(AuthService auth, String username, String password) {
        return auth.loginManager(username, password);
    }

    @Override
    public void changePassword(AuthService auth, String username, String current, String next) {
        auth.changeManagerPassword(username, current, next);
    }

    @Override
    public Set<LoanStatus> loansToDecide() {
        return EnumSet.of(LoanStatus.FINAL_STEP);
    }

    @Override
    public Set<LoanStatus> loansInProgress() {
        return EnumSet.of(LoanStatus.FINAL_STEP);
    }

    @Override
    public Set<LoanStatus> loanHistory() {
        return EnumSet.of(LoanStatus.DENIED, LoanStatus.ACCEPTED);
    }

    @Override
    public void decideLoan(LoanService loans, long loanId, boolean approve) {
        loans.managerDecision(loanId, approve);
    }

    @Override
    public String reviewedChequesLabel() {
        return "Approved Checks";
    }
}
