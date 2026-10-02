package com.example.bank.service;

import com.example.bank.model.Account;
import com.example.bank.model.AccountStatus;
import com.example.bank.model.Employee;
import com.example.bank.repository.AccountRepository;
import com.example.bank.repository.EmployeeRepository;
import com.example.bank.security.PasswordHasher;

import java.util.List;
import java.util.Optional;

/** Signing in and changing passwords, for customers and for staff. */
public final class AuthService {

    private final AccountRepository accounts;
    private final EmployeeRepository employees;
    private final EmployeeRepository managers;
    private final PasswordHasher hasher;

    public AuthService(AccountRepository accounts, EmployeeRepository employees, EmployeeRepository managers, PasswordHasher hasher) {
        this.accounts = accounts;
        this.employees = employees;
        this.managers = managers;
        this.hasher = hasher;
    }

    /** The result of a customer sign-in attempt. */
    public record CustomerLogin(String username, String fullName, AccountStatus status) {
        public boolean approved() {
            return status == AccountStatus.ACCEPTED;
        }
    }

    public Optional<CustomerLogin> loginCustomer(String username, String password) {
        String stored = accounts.passwordHashOf(username);
        if (stored == null || !hasher.matches(password, stored)) return Optional.empty();
        if (hasher.needsRehash(stored)) accounts.updatePasswordHash(username, hasher.hash(password));
        List<Account> owned = accounts.findByUsername(username);
        if (owned.isEmpty()) return Optional.empty();
        boolean anyAccepted = owned.stream().anyMatch(Account::isUsable);
        return Optional.of(new CustomerLogin(username, owned.get(0).fullName(), anyAccepted ? AccountStatus.ACCEPTED : owned.get(0).status()));
    }

    public Optional<Employee> loginEmployee(String username, String password) {
        return loginStaff(employees, username, password);
    }

    /** Managers sign in against their own table. The old manager app checked the employee table, so every employee could open it. */
    public Optional<Employee> loginManager(String username, String password) {
        return loginStaff(managers, username, password);
    }

    private Optional<Employee> loginStaff(EmployeeRepository repo, String username, String password) {
        String stored = repo.passwordHashOf(username);
        if (stored == null || !hasher.matches(password, stored)) return Optional.empty();
        if (hasher.needsRehash(stored)) repo.updatePasswordHash(username, hasher.hash(password));
        return repo.find(username);
    }

    public void changeCustomerPassword(String username, String current, String next) {
        requireStrong(next);
        String stored = accounts.passwordHashOf(username);
        if (stored == null || !hasher.matches(current, stored)) throw new BankException("The current password is wrong.");
        accounts.updatePasswordHash(username, hasher.hash(next));
    }

    public void changeEmployeePassword(String username, String current, String next) {
        changeStaffPassword(employees, username, current, next);
    }

    public void changeManagerPassword(String username, String current, String next) {
        changeStaffPassword(managers, username, current, next);
    }

    private void changeStaffPassword(EmployeeRepository repo, String username, String current, String next) {
        requireStrong(next);
        String stored = repo.passwordHashOf(username);
        if (stored == null || !hasher.matches(current, stored)) throw new BankException("The current password is wrong.");
        repo.updatePasswordHash(username, hasher.hash(next));
    }

    public String hash(String password) {
        requireStrong(password);
        return hasher.hash(password);
    }

    static void requireStrong(String password) {
        if (password == null || password.length() < 8) throw new BankException("The password must be at least 8 characters.");
        if (!password.matches(".*\\d.*") || !password.matches(".*[A-Za-z].*")) {
            throw new BankException("The password must contain letters and numbers.");
        }
    }
}
