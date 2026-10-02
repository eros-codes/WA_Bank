package com.example.customer;

/** Every screen and card of the customer app, with its FXML file. */
public enum CustomerScreen {
    WELCOME("welcome.fxml", "Welcome"),
    DASHBOARD("dashboard.fxml", "Home"),
    TRANSFERS("enteghalevajh.fxml", "Transfers"),
    PAYMENTS("billpayment.fxml", "Bills and top-ups"),
    CHEQUE_BOOK("checkbook.fxml", "Write a cheque"),
    MY_CHEQUES("mycheckbooks.fxml", "My cheques"),
    LOAN("loan.fxml", "Loan"),
    OPEN_DEPOSIT("deposit.fxml", "Open a deposit"),
    MY_DEPOSITS("mydeposits.fxml", "My deposits"),
    DEPOSIT_DETAILS("depositdetails.fxml", "Deposit"),
    HISTORY("mytransactions.fxml", "My transactions"),
    ACCOUNTS("myaccounts.fxml", "My accounts"),
    NEW_ACCOUNT("addaccount.fxml", "Open an account"),
    PROFILE("editpass.fxml", "My profile"),
    ACCOUNT_CARD("myaccountitem.fxml", ""),
    HISTORY_ROW("transactionsitem.fxml", ""),
    CHEQUE_ROW("checkbookitems.fxml", ""),
    DEPOSIT_CARD("mydeposititem.fxml", "");

    private final String file;
    private final String title;

    CustomerScreen(String file, String title) {
        this.file = file;
        this.title = title;
    }

    public String resource() {
        return "/com/example/bank/" + file;
    }

    public String title() {
        return title;
    }
}
