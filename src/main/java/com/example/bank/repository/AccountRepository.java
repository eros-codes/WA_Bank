package com.example.bank.repository;

import com.example.bank.model.Account;
import com.example.bank.model.AccountStatus;
import com.example.bank.model.CustomerProfile;
import com.example.bank.model.Money;
import com.example.bank.model.NewAccountRequest;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Customers and their accounts (the userinfo table). */
public interface AccountRepository {

    List<Account> findByUsername(String username);

    /** The account card operations use: the customer's accepted checking account. */
    Optional<Account> findPrimary(String username);

    Optional<Account> findByAccountNumber(String accountNumber);

    Optional<Account> findByCardNumber(String cardNumber);

    Optional<Account> findByIban(String iban);

    List<Account> findByStatus(AccountStatus status);

    /** Accounts with their owners' details, for the staff lists. */
    List<AccountOverview> overview(AccountStatus status);

    /** Removes every account a customer has. */
    void deleteCustomer(String username);

    Optional<CustomerProfile> findProfile(String username);

    /** The oldest account still waiting for review, with its profile and ID photo. */
    Optional<PendingAccount> findNextPending();

    boolean nationalIdExists(String nationalId);

    /** Numbers that must be unique across the bank. */
    enum Identifier { USERNAME, CARD, IBAN, ACCOUNT }

    boolean isTaken(Identifier kind, String value);

    boolean fullNameExists(String fullName);

    String passwordHashOf(String username);

    void updatePasswordHash(String username, String hash);

    void create(NewAccountRequest request, String passwordHash);

    /** Approves one account that is waiting for review; false if it was not waiting any more. */
    boolean approvePending(String accountNumber);

    /** Removes one account that is waiting for review. Never touches accounts already approved. */
    boolean deletePending(String accountNumber);

    void deleteAccount(String accountNumber);

    void updateProfile(CustomerProfile profile);

    /** Takes money only if it is there; false when the balance is too low. Must run inside a transaction. */
    boolean debit(Connection c, String accountNumber, Money amount) throws SQLException;

    /** Adds money; false when there is no such account. Must run inside a transaction. */
    boolean credit(Connection c, String accountNumber, Money amount) throws SQLException;

    record AccountOverview(Account account, CustomerProfile profile) {
    }

    record PendingAccount(CustomerProfile profile, Account account, byte[] idPhoto) {
    }
}
