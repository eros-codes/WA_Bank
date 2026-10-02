package com.example.bank.service;

import com.example.bank.db.Database;
import com.example.bank.model.Account;
import com.example.bank.model.AccountStatus;
import com.example.bank.model.AccountType;
import com.example.bank.model.CustomerProfile;
import com.example.bank.model.Money;
import com.example.bank.model.NewAccountRequest;
import com.example.bank.repository.AccountRepository;

import java.util.List;
import java.util.Optional;

/** Customers and their accounts: signing up, review, profile changes, closing, salaries. */
public final class CustomerService {

    private final Database db;
    private final AccountRepository accounts;
    private final AuthService auth;
    private final AccountFactory factory;

    public CustomerService(Database db, AccountRepository accounts, AuthService auth, AccountFactory factory) {
        this.db = db;
        this.accounts = accounts;
        this.auth = auth;
        this.factory = factory;
    }

    /** What a new customer is told once: their username, their first password and their new account. */
    public record Registration(String username, String password, Account account) {
    }

    /**
     * Registers a new customer. The profile's username is ignored: a fresh,
     * unique one is created, with a random first password shown once.
     */
    public Registration signUp(CustomerProfile details, AccountType type, Money openingBalance, byte[] idPhoto) {
        SignUpRules.nationalId(details.nationalId());
        if (idPhoto == null || idPhoto.length == 0) throw new BankException("Upload a photo of your national ID card.");
        if (openingBalance.compareTo(MINIMUM_OPENING) < 0) throw new BankException("The first deposit must be at least " + MINIMUM_OPENING.format() + " IRR.");
        if (accounts.nationalIdExists(details.nationalId())) throw new BankException("An account with this national ID already exists.");
        String username = factory.newUsername();
        String password = factory.newPassword();
        CustomerProfile p = new CustomerProfile(username, details.fullName(), details.nationalId(), details.birthDate(), details.gender(),
                details.maritalStatus(), details.nationality(), details.address(), details.postalCode(), details.phoneNumber(),
                details.email(), details.occupation(), details.employerAddress(), details.annualIncome(), details.creationDate());
        Account account = factory.newAccount(username, p.fullName(), type, openingBalance);
        accounts.create(new NewAccountRequest(p, password, account, idPhoto), auth.hash(password));
        return new Registration(username, password, account);
    }

    /** A further account for an existing customer: new numbers, waiting for review. */
    public Account openAnotherAccount(String username, AccountType type, Money openingBalance) {
        CustomerProfile p = accounts.findProfile(username).orElseThrow(() -> new NotFoundException("No such customer."));
        Account account = factory.newAccount(username, p.fullName(), type, openingBalance);
        accounts.create(new NewAccountRequest(p, null, account, null), accounts.passwordHashOf(username));
        return account;
    }

    public static final Money MINIMUM_OPENING = Money.of(50_000);

    public List<Account> accountsOf(String username) {
        return accounts.findByUsername(username);
    }

    public Optional<Account> primaryAccount(String username) {
        return accounts.findPrimary(username);
    }

    public Optional<CustomerProfile> profile(String username) {
        return accounts.findProfile(username);
    }

    public Optional<AccountRepository.PendingAccount> nextPending() {
        return accounts.findNextPending();
    }

    public void approve(String accountNumber) {
        if (!accounts.approvePending(accountNumber)) throw new BankException("This account has already been reviewed.");
    }

    public void deny(String accountNumber) {
        if (!accounts.deletePending(accountNumber)) throw new BankException("This account has already been reviewed.");
    }

    public List<Account> approvedAccounts() {
        return accounts.findByStatus(AccountStatus.ACCEPTED);
    }

    public void updateProfile(CustomerProfile profile) {
        accounts.updateProfile(profile);
    }

    /** Closing an account that still holds money would make the money disappear, so it is refused. */
    public void closeAccount(String username, String accountNumber) {
        Account a = accounts.findByAccountNumber(accountNumber).filter(x -> x.username().equals(username))
                .orElseThrow(() -> new NotFoundException("No such account."));
        if (a.balance().isPositive()) throw new BankException("Move the money out of this account before closing it.");
        accounts.deleteAccount(accountNumber);
    }

    public List<AccountRepository.AccountOverview> approvedOverview() {
        return accounts.overview(AccountStatus.ACCEPTED);
    }

    /** Removes a customer entirely; refused while any of their accounts holds money. */
    public void removeCustomer(String username) {
        List<Account> owned = accounts.findByUsername(username);
        if (owned.isEmpty()) throw new NotFoundException("No such customer.");
        if (owned.stream().anyMatch(a -> a.balance().isPositive())) {
            throw new BankException("This customer still has money in an account. It has to be paid out before the customer can be removed.");
        }
        accounts.deleteCustomer(username);
    }

    public boolean profileByNationalIdExists(String nationalId) {
        return accounts.nationalIdExists(nationalId);
    }

    public boolean customerNamed(String fullName) {
        return accounts.fullNameExists(fullName);
    }
}
