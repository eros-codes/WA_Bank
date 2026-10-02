package com.example.bank.service;

import com.example.bank.db.Database;
import com.example.bank.model.Account;
import com.example.bank.model.Employee;
import com.example.bank.model.Money;
import com.example.bank.repository.AccountRepository;
import com.example.bank.repository.BankFundsRepository;
import com.example.bank.repository.EmployeeRepository;

import java.util.List;

/** The manager's view of staff: who works here, hiring, salaries and letting go. */
public final class StaffService {

    private final Database db;
    private final EmployeeRepository employees;
    private final AccountRepository accounts;
    private final BankFundsRepository funds;

    public StaffService(Database db, EmployeeRepository employees, AccountRepository accounts, BankFundsRepository funds) {
        this.db = db;
        this.employees = employees;
        this.accounts = accounts;
        this.funds = funds;
    }

    public List<Employee> all() {
        return employees.findAll();
    }

    /** Makes a customer an employee, with the same username and password. */
    public void promote(String customerUsername) {
        if (employees.find(customerUsername).isPresent()) throw new BankException("This person is already an employee.");
        String name = accounts.findProfile(customerUsername).orElseThrow(() -> new NotFoundException("No such customer.")).fullName();
        employees.create(new Employee(customerUsername, name), accounts.passwordHashOf(customerUsername));
    }

    public void remove(String username) {
        employees.delete(username);
    }

    /**
     * Pays a salary from the bank's funds into the employee's own checking
     * account (an employee banks here under the same username). The bank's
     * funds and the account change together or not at all.
     */
    public void paySalary(String username, Money amount) {
        if (!amount.isPositive()) throw new BankException("Enter the salary amount.");
        Account to = accounts.findPrimary(username)
                .orElseThrow(() -> new BankException("This employee has no approved checking account to be paid into."));
        db.inTransaction(c -> {
            if (!funds.take(c, amount)) throw new BankException("The bank does not have enough funds for this salary.");
            if (!accounts.credit(c, to.accountNumber(), amount)) throw new NotFoundException("The account no longer exists.");
            return null;
        });
    }

    public Money bankFunds() {
        return funds.balance();
    }
}
