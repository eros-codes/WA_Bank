package com.example.bank.service;

import com.example.bank.db.Database;
import com.example.bank.model.Account;
import com.example.bank.model.TermDeposit;
import com.example.bank.repository.AccountRepository;
import com.example.bank.repository.DepositRepository;

import java.util.List;

/**
 * Opening and closing fixed-term deposits. Opening takes the rials from the
 * customer's chosen account and records the deposit in one transaction;
 * closing returns its value in rials (a foreign-currency deposit is
 * converted back) in one transaction.
 */
public final class DepositService {

    private final Database db;
    private final DepositRepository deposits;
    private final AccountRepository accounts;

    public DepositService(Database db, DepositRepository deposits, AccountRepository accounts) {
        this.db = db;
        this.deposits = deposits;
        this.accounts = accounts;
    }

    /** @param source one of the customer's own card or account numbers */
    public TermDeposit open(String username, String source, TermDeposit request) {
        if (!request.amount().isPositive()) throw new BankException("Enter the amount to deposit.");
        Account from = ownAccount(username, source);
        return db.inTransaction(c -> {
            if (!accounts.debit(c, from.accountNumber(), request.valueInRials())) throw new InsufficientFundsException();
            int number = deposits.create(c, request, from.accountNumber());
            return new TermDeposit(number, request.username(), from.fullName(), request.type(), request.amount(), request.duration(),
                    request.interest(), request.payment(), request.currency(), request.date());
        });
    }

    /** Closes a deposit and returns its value in rials to the customer's checking account. */
    public void close(String username, int number) {
        TermDeposit d = deposits.findByNumber(number).filter(x -> x.username().equals(username))
                .orElseThrow(() -> new NotFoundException("No such deposit."));
        Account to = accounts.findPrimary(username).orElseThrow(() -> new BankException("You need an approved checking account."));
        db.inTransaction(c -> {
            if (!deposits.delete(c, number)) throw new BankException("This deposit has already been closed.");
            if (!accounts.credit(c, to.accountNumber(), d.valueInRials())) throw new NotFoundException("Your account no longer exists.");
            return null;
        });
    }

    private Account ownAccount(String username, String source) {
        String s = source == null ? "" : source.replaceAll("\\s", "");
        return accounts.findByCardNumber(s).or(() -> accounts.findByAccountNumber(s))
                .filter(a -> a.username().equals(username) && a.isUsable())
                .orElseThrow(() -> new BankException("Enter one of your own approved card or account numbers."));
    }

    public List<TermDeposit> of(String username) {
        return deposits.findByUsername(username);
    }

    public List<TermDeposit> all() {
        return deposits.findAll();
    }

    public TermDeposit find(String username, int number) {
        return deposits.findByNumber(number).filter(d -> d.username().equals(username))
                .orElseThrow(() -> new NotFoundException("No such deposit."));
    }
}
