package com.example.bank.repository.jdbc;

import com.example.bank.db.Database;
import com.example.bank.model.Account;
import com.example.bank.model.AccountStatus;
import com.example.bank.model.AccountType;
import com.example.bank.model.CustomerProfile;
import com.example.bank.model.Money;
import com.example.bank.model.NewAccountRequest;
import com.example.bank.repository.AccountRepository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/** Customers and their accounts: the users, user_roles and accounts tables. */
public final class JdbcAccountRepository implements AccountRepository {

    /** An account with its owner's username and name. Closed accounts are left out everywhere. */
    private static final String ACCOUNTS = "SELECT u.username, u.full_name, a.account_number, a.account_type, a.statement_status, "
            + "a.card_number, a.cvv, a.iban, DATE_FORMAT(a.expire_date, '%Y.%m') AS expiry, a.balance "
            + "FROM accounts a JOIN users u ON u.user_id = a.user_id WHERE a.statement_status <> 'closed'";
    private static final String PROFILE = "SELECT u.username, u.full_name, u.national_id, DATE_FORMAT(u.birth_date, '%Y.%m.%d') AS birth, "
            + "u.gender, u.marital_status, u.nationality, u.address, u.postal_code, u.phone_number, u.email, u.occupation, "
            + "u.employer_address, u.annual_income, DATE_FORMAT(u.created_at, '%Y-%m-%d %H:%i') AS created FROM users u";

    private final Database db;

    public JdbcAccountRepository(Database db) {
        this.db = db;
    }

    static Account account(ResultSet r) throws SQLException {
        return new Account(r.getString("username"), r.getString("full_name"), r.getString("account_number"),
                AccountType.fromDb(r.getString("account_type")), AccountStatus.fromDb(r.getString("statement_status")),
                r.getString("card_number"), r.getString("cvv"), r.getString("iban"), r.getString("expiry"),
                new Money(r.getBigDecimal("balance")));
    }

    static CustomerProfile profile(ResultSet r) throws SQLException {
        BigDecimal income = r.getBigDecimal("annual_income");
        return new CustomerProfile(r.getString("username"), r.getString("full_name"), r.getString("national_id"), r.getString("birth"),
                r.getString("gender"), r.getString("marital_status"), r.getString("nationality"), r.getString("address"),
                r.getString("postal_code"), r.getString("phone_number"), r.getString("email"), r.getString("occupation"),
                r.getString("employer_address"), income == null ? null : income.stripTrailingZeros().toPlainString(), r.getString("created"));
    }

    @Override
    public List<Account> findByUsername(String username) {
        return db.query(ACCOUNTS + " AND u.username = ? ORDER BY a.account_id", JdbcAccountRepository::account, username);
    }

    @Override
    public Optional<Account> findPrimary(String username) {
        return db.queryOne(ACCOUNTS + " AND u.username = ? AND a.statement_status = ? AND a.account_type = ? ORDER BY a.account_id LIMIT 1",
                JdbcAccountRepository::account, username, AccountStatus.ACCEPTED.dbValue(), AccountType.CHECKING.dbValue());
    }

    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) {
        return db.queryOne(ACCOUNTS + " AND a.account_number = ?", JdbcAccountRepository::account, accountNumber);
    }

    @Override
    public Optional<Account> findByCardNumber(String cardNumber) {
        return db.queryOne(ACCOUNTS + " AND a.card_number = ?", JdbcAccountRepository::account, cardNumber);
    }

    @Override
    public Optional<Account> findByIban(String iban) {
        return db.queryOne(ACCOUNTS + " AND a.iban = ?", JdbcAccountRepository::account, iban);
    }

    @Override
    public List<Account> findByStatus(AccountStatus status) {
        return db.query(ACCOUNTS + " AND a.statement_status = ? ORDER BY a.account_id", JdbcAccountRepository::account, status.dbValue());
    }

    @Override
    public List<AccountOverview> overview(AccountStatus status) {
        return db.query("SELECT u.username, u.full_name, u.national_id, DATE_FORMAT(u.birth_date, '%Y.%m.%d') AS birth, u.gender, "
                        + "u.marital_status, u.nationality, u.address, u.postal_code, u.phone_number, u.email, u.occupation, "
                        + "u.employer_address, u.annual_income, DATE_FORMAT(u.created_at, '%Y-%m-%d %H:%i') AS created, "
                        + "a.account_number, a.account_type, a.statement_status, a.card_number, a.cvv, a.iban, "
                        + "DATE_FORMAT(a.expire_date, '%Y.%m') AS expiry, a.balance "
                        + "FROM accounts a JOIN users u ON u.user_id = a.user_id WHERE a.statement_status = ? ORDER BY u.full_name",
                r -> new AccountOverview(account(r), profile(r)), status.dbValue());
    }

    @Override
    public void deleteCustomer(String username) {
        // closed, not deleted: their transfers and cheques refer to their accounts
        db.inTransaction(c -> {
            Database.update(c, "UPDATE accounts a JOIN users u ON u.user_id = a.user_id SET a.statement_status = 'closed' WHERE u.username = ?", username);
            Database.update(c, "UPDATE users SET status = 'closed' WHERE username = ?", username);
            return null;
        });
    }

    @Override
    public Optional<CustomerProfile> findProfile(String username) {
        return db.queryOne(PROFILE + " WHERE u.username = ?", JdbcAccountRepository::profile, username);
    }

    @Override
    public Optional<PendingAccount> findNextPending() {
        return db.queryOne("SELECT u.username, u.full_name, u.national_id, DATE_FORMAT(u.birth_date, '%Y.%m.%d') AS birth, u.gender, "
                        + "u.marital_status, u.nationality, u.address, u.postal_code, u.phone_number, u.email, u.occupation, "
                        + "u.employer_address, u.annual_income, DATE_FORMAT(u.created_at, '%Y-%m-%d %H:%i') AS created, u.id_photo, "
                        + "a.account_number, a.account_type, a.statement_status, a.card_number, a.cvv, a.iban, "
                        + "DATE_FORMAT(a.expire_date, '%Y.%m') AS expiry, a.balance "
                        + "FROM accounts a JOIN users u ON u.user_id = a.user_id WHERE a.statement_status = ? ORDER BY a.account_id LIMIT 1",
                r -> new PendingAccount(profile(r), account(r), r.getBytes("id_photo")), AccountStatus.PENDING.dbValue());
    }

    @Override
    public boolean nationalIdExists(String nationalId) {
        return db.queryOne("SELECT 1 FROM users WHERE national_id = ? LIMIT 1", r -> true, nationalId).isPresent();
    }

    @Override
    public boolean isTaken(Identifier kind, String value) {
        String sql = switch (kind) {
            case USERNAME -> "SELECT 1 FROM users WHERE username = ? LIMIT 1";
            case CARD -> "SELECT 1 FROM accounts WHERE card_number = ? LIMIT 1";
            case IBAN -> "SELECT 1 FROM accounts WHERE iban = ? LIMIT 1";
            case ACCOUNT -> "SELECT 1 FROM accounts WHERE account_number = ? LIMIT 1";
        };
        return db.queryOne(sql, r -> true, value).isPresent();
    }

    @Override
    public boolean fullNameExists(String fullName) {
        return db.queryOne("SELECT 1 FROM users u JOIN accounts a ON a.user_id = u.user_id WHERE u.full_name = ? AND a.statement_status = 'active' LIMIT 1",
                r -> true, fullName).isPresent();
    }

    @Override
    public String passwordHashOf(String username) {
        return db.queryOne("SELECT password_hash FROM users WHERE username = ? AND status = 'active'", r -> r.getString(1), username).orElse(null);
    }

    @Override
    public void updatePasswordHash(String username, String hash) {
        db.update("UPDATE users SET password_hash = ? WHERE username = ?", hash, username);
    }

    /** A new customer (user, CUSTOMER role, first account) or a further account for an existing one, in one transaction. */
    @Override
    public void create(NewAccountRequest request, String passwordHash) {
        CustomerProfile p = request.profile();
        Account a = request.account();
        db.inTransaction(c -> {
            Integer userId = Database.queryOne(c, "SELECT user_id FROM users WHERE username = ?", r -> r.getInt(1), p.username()).orElse(null);
            if (userId == null) {
                Database.update(c, "INSERT INTO users (username, password_hash, full_name, national_id, birth_date, gender, marital_status, "
                                + "nationality, address, postal_code, phone_number, email, occupation, employer_address, annual_income, id_photo) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        p.username(), passwordHash, p.fullName(), p.nationalId(), date(p.birthDate()), p.gender(), p.maritalStatus(),
                        p.nationality(), p.address(), p.postalCode(), p.phoneNumber(), p.email(), p.occupation(), p.employerAddress(),
                        number(p.annualIncome()), request.idPhoto());
                userId = Database.queryOne(c, "SELECT user_id FROM users WHERE username = ?", r -> r.getInt(1), p.username()).orElseThrow();
                Database.update(c, "INSERT INTO user_roles (user_id, role_id) SELECT ?, role_id FROM roles WHERE role_name = 'CUSTOMER'", userId);
            }
            Database.update(c, "INSERT INTO accounts (user_id, account_number, card_number, iban, cvv, expire_date, account_type, balance, statement_status) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    userId, a.accountNumber(), a.cardNumber(), a.iban(), a.cvv(), expiryDate(a.expiryDate()), a.type().dbValue(),
                    a.balance().amount(), a.status().dbValue());
            return null;
        });
    }

    @Override
    public boolean approvePending(String accountNumber) {
        return db.update("UPDATE accounts SET statement_status = ? WHERE account_number = ? AND statement_status = ?",
                AccountStatus.ACCEPTED.dbValue(), accountNumber, AccountStatus.PENDING.dbValue()) == 1;
    }

    /** Removes an account waiting for review, and the person too if it was all they had here (a denied sign-up). */
    @Override
    public boolean deletePending(String accountNumber) {
        return db.inTransaction(c -> {
            Integer userId = Database.queryOne(c, "SELECT user_id FROM accounts WHERE account_number = ? AND statement_status = ?",
                    r -> r.getInt(1), accountNumber, AccountStatus.PENDING.dbValue()).orElse(null);
            if (userId == null) return false;
            Database.update(c, "DELETE FROM accounts WHERE account_number = ? AND statement_status = ?", accountNumber, AccountStatus.PENDING.dbValue());
            boolean nothingLeft = Database.queryOne(c, "SELECT 1 FROM accounts WHERE user_id = ? LIMIT 1", r -> true, userId).isEmpty()
                    && Database.queryOne(c, "SELECT 1 FROM user_roles ur JOIN roles r ON r.role_id = ur.role_id "
                    + "WHERE ur.user_id = ? AND r.role_name <> 'CUSTOMER' LIMIT 1", r -> true, userId).isEmpty();
            if (nothingLeft) Database.update(c, "DELETE FROM users WHERE user_id = ?", userId);
            return true;
        });
    }

    @Override
    public void deleteAccount(String accountNumber) {
        db.update("UPDATE accounts SET statement_status = 'closed' WHERE account_number = ?", accountNumber);
    }

    @Override
    public void updateProfile(CustomerProfile p) {
        db.update("UPDATE users SET full_name = ?, gender = ?, marital_status = ?, address = ?, postal_code = ?, phone_number = ?, "
                        + "email = ?, occupation = ?, employer_address = ? WHERE username = ?",
                p.fullName(), p.gender(), p.maritalStatus(), p.address(), p.postalCode(), blankToNull(p.phoneNumber()), blankToNull(p.email()),
                p.occupation(), p.employerAddress(), p.username());
    }

    @Override
    public boolean debit(Connection c, String accountNumber, Money amount) throws SQLException {
        return Database.update(c, "UPDATE accounts SET balance = balance - ? WHERE account_number = ? AND statement_status = 'active' AND balance >= ?",
                amount.amount(), accountNumber, amount.amount()) == 1;
    }

    @Override
    public boolean credit(Connection c, String accountNumber, Money amount) throws SQLException {
        return Database.update(c, "UPDATE accounts SET balance = balance + ? WHERE account_number = ? AND statement_status = 'active'",
                amount.amount(), accountNumber) == 1;
    }

    /* ---- the app's text formats to the database's types ---- */

    /** "yyyy.MM" to the last day of that month. */
    static Date expiryDate(String expiry) {
        String[] p = expiry.split("[./-]");
        return Date.valueOf(YearMonth.of(Integer.parseInt(p[0]), Integer.parseInt(p[1])).atEndOfMonth());
    }

    /** "yyyy.M.d" (or with - or /) to a date, or null when it is not one. */
    static Date date(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            return Date.valueOf(LocalDate.parse(text.strip().replace('/', '.').replace('-', '.'), DateTimeFormatter.ofPattern("y.M.d")));
        } catch (RuntimeException e) {
            return null;
        }
    }

    static BigDecimal number(String text) {
        return text == null || !text.strip().matches("\\d+(\\.\\d+)?") ? null : new BigDecimal(text.strip());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }
}
