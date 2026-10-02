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

/** An employee: does the first review of loans, and cannot use the manager's screens. */
public final class EmployeeRole implements StaffRole {

    public static final EmployeeRole INSTANCE = new EmployeeRole();

    private static final Map<Page, String> FILES = new EnumMap<>(Page.class);

    static {
        FILES.put(Page.LOGIN, "login.fxml");
        FILES.put(Page.DASHBOARD, "dashboardworker.fxml");
        FILES.put(Page.VERIFY, "verify.fxml");
        FILES.put(Page.PAYMENTS, "bill payments.fxml");
        FILES.put(Page.CHANGE_PASSWORD, "change password.fxml");
        FILES.put(Page.REPORTS, "financial reports.fxml");
        FILES.put(Page.ACCOUNTS, "list of accounts.fxml");
        FILES.put(Page.DEPOSITS, "despositworker.fxml");
        FILES.put(Page.TRANSACTIONS, "transactions.fxml");
        FILES.put(Page.LOANS, "loan requests.fxml");
        FILES.put(Page.LOAN_CARD, "loanhome.fxml");
        FILES.put(Page.DECIDED_LOAN_CARD, "loanstatu.fxml");
        FILES.put(Page.TRANSFER_CARD, "Card by card.fxml");
        FILES.put(Page.CHEQUE_REVIEW_CARD, "check.fxml");
        FILES.put(Page.REVIEWED_CHEQUE_CARD, "check 1.fxml");
        FILES.put(Page.BILL_CARD, "bill.fxml");
        FILES.put(Page.TOP_UP_CARD, "charge.fxml");
        FILES.put(Page.DEPOSIT_CARD, "desposit2.fxml");
    }

    private EmployeeRole() {
    }

    @Override
    public String name() {
        return "Employee";
    }

    @Override
    public String resource(Page page) {
        String file = FILES.get(page);
        if (file == null) throw new IllegalStateException("Employees have no " + page + " screen.");
        return "/com/example/worker/" + file;
    }

    @Override
    public Optional<Employee> signIn(AuthService auth, String username, String password) {
        return auth.loginEmployee(username, password);
    }

    @Override
    public void changePassword(AuthService auth, String username, String current, String next) {
        auth.changeEmployeePassword(username, current, next);
    }

    @Override
    public Set<LoanStatus> loansToDecide() {
        return EnumSet.of(LoanStatus.PENDING);
    }

    @Override
    public Set<LoanStatus> loansInProgress() {
        return EnumSet.of(LoanStatus.PENDING, LoanStatus.FINAL_STEP);
    }

    @Override
    public Set<LoanStatus> loanHistory() {
        return EnumSet.of(LoanStatus.FINAL_STEP, LoanStatus.DENIED, LoanStatus.ACCEPTED);
    }

    @Override
    public void decideLoan(LoanService loans, long loanId, boolean approve) {
        loans.employeeReview(loanId, approve);
    }

    @Override
    public String reviewedChequesLabel() {
        return "Checks";
    }
}
