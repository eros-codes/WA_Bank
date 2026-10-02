package com.example.staff;

/** Every screen and card of the staff apps. Each role maps them to its own FXML files. */
public enum Page {
    LOGIN("Sign in"),
    DASHBOARD("Work area"),
    VERIFY("Verify accounts"),
    PAYMENTS("Bills and top-ups"),
    CHANGE_PASSWORD("Change password"),
    REPORTS("Financial reports"),
    ACCOUNTS("Accounts"),
    DEPOSITS("Deposits"),
    TRANSACTIONS("Transactions"),
    LOANS("Loan requests"),
    // cards and small windows
    LOAN_CARD(""), DECIDED_LOAN_CARD(""), TRANSFER_CARD(""), CHEQUE_REVIEW_CARD(""), REVIEWED_CHEQUE_CARD(""),
    BILL_CARD(""), TOP_UP_CARD(""), DEPOSIT_CARD(""), CUSTOMER_ROW(""), EMPLOYEE_ROW(""), MANAGE_CUSTOMER("Manage account");

    private final String title;

    Page(String title) {
        this.title = title;
    }

    public String title() {
        return title;
    }
}
