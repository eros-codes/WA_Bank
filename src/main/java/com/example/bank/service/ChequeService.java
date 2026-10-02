package com.example.bank.service;

import com.example.bank.model.Account;
import com.example.bank.model.Cheque;
import com.example.bank.model.ChequeStatus;
import com.example.bank.model.Money;
import com.example.bank.repository.AccountRepository;
import com.example.bank.repository.ChequeRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/** Writing cheques and reviewing them. */
public final class ChequeService {

    private final ChequeRepository cheques;
    private final AccountRepository accounts;
    private final Clock clock;

    public ChequeService(ChequeRepository cheques, AccountRepository accounts, Clock clock) {
        this.cheques = cheques;
        this.accounts = accounts;
        this.clock = clock;
    }

    /** Writes a cheque and returns its number. */
    public int write(String username, String recipientName, Money amount, LocalDateTime dueDate) {
        if (!amount.isPositive()) throw new BankException("Enter the amount of the cheque.");
        Account from = accounts.findPrimary(username).orElseThrow(() -> new BankException("You need an approved checking account to write cheques."));
        if (!from.balance().isAtLeast(amount)) throw new InsufficientFundsException();
        if (!accounts.fullNameExists(recipientName.strip())) throw new NotFoundException("There is no customer with that name.");
        LocalDateTime now = LocalDateTime.now(clock);
        if (dueDate.isBefore(now)) throw new BankException("The due date cannot be in the past.");
        return cheques.create(new Cheque(0, from.fullName(), amount, recipientName.strip(), now, dueDate, ChequeStatus.PENDING), from.accountNumber());
    }

    public List<Cheque> writtenBy(String username) {
        return cheques.findByIssuer(username);
    }

    public List<Cheque> pending() {
        return cheques.findPending();
    }

    public List<Cheque> reviewed() {
        return cheques.findReviewed();
    }

    public void review(int number, boolean accept) {
        if (!cheques.review(number, accept ? ChequeStatus.ACCEPTED : ChequeStatus.DENIED)) throw new BankException("This cheque has already been reviewed.");
    }
}
