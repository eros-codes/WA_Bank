package com.example.bank.service;

import com.example.bank.db.Database;
import com.example.bank.model.Account;
import com.example.bank.model.Loan;
import com.example.bank.model.LoanApplication;
import com.example.bank.model.LoanStatus;
import com.example.bank.repository.AccountRepository;
import com.example.bank.repository.LoanRepository;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * The loan process: a customer applies, an employee does the first review,
 * the manager makes the final decision and, if it is yes, the money is paid
 * into the customer's account in the same transaction.
 */
public final class LoanService {

    private final Database db;
    private final LoanRepository loans;
    private final AccountRepository accounts;

    public LoanService(Database db, LoanRepository loans, AccountRepository accounts) {
        this.db = db;
        this.loans = loans;
        this.accounts = accounts;
    }

    public long apply(LoanApplication application) {
        if (!application.amount().isPositive()) throw new BankException("Enter the amount you want to borrow.");
        if (application.idDocument() == null || application.employmentDocument() == null
                || application.collateralDocument() == null || application.deedDocument() == null) {
            throw new BankException("All four documents are needed.");
        }
        return loans.create(application);
    }

    public List<Loan> list(Set<LoanStatus> statuses) {
        return loans.findByStatus(statuses);
    }

    public int count(Set<LoanStatus> statuses) {
        return loans.count(statuses);
    }

    public int countAll() {
        return loans.countAll();
    }

    /** First review by an employee: forward to the manager, or deny. */
    public void employeeReview(long loanId, boolean approve) {
        LoanStatus to = approve ? LoanStatus.FINAL_STEP : LoanStatus.DENIED;
        db.inTransaction(c -> {
            if (!loans.changeStatus(c, loanId, LoanStatus.PENDING, to)) throw new BankException("This request has already been reviewed.");
            return null;
        });
    }

    /** The manager's decision. Accepting pays the loan into the customer's checking account. */
    public void managerDecision(long loanId, boolean approve) {
        Loan loan = loans.findById(loanId).orElseThrow(() -> new NotFoundException("No such loan request."));
        if (!approve) {
            db.inTransaction(c -> {
                if (!loans.changeStatus(c, loanId, LoanStatus.FINAL_STEP, LoanStatus.DENIED)) throw new BankException("This request has already been decided.");
                return null;
            });
            return;
        }
        Account payTo = accounts.findPrimary(loan.username())
                .orElseThrow(() -> new BankException("The customer has no approved checking account to pay the loan into."));
        db.inTransaction(c -> {
            if (!loans.changeStatus(c, loanId, LoanStatus.FINAL_STEP, LoanStatus.ACCEPTED)) throw new BankException("This request has already been decided.");
            if (!accounts.credit(c, payTo.accountNumber(), loan.amount())) throw new NotFoundException("The customer's account no longer exists.");
            return null;
        });
    }

    public static final Set<LoanStatus> DECIDED = EnumSet.of(LoanStatus.ACCEPTED, LoanStatus.DENIED);
}
