package com.example.bank.service;

import com.example.bank.model.Account;
import com.example.bank.model.AccountStatus;
import com.example.bank.model.AccountType;
import com.example.bank.model.Money;
import com.example.bank.repository.AccountRepository;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * Creates new accounts with numbers nobody else has. Before, the username was
 * the last six digits of the IBAN (with no check that it was free), the first
 * password was the last four digits of the national ID, and the card's
 * expiry was the expiry of the customer's ID card.
 */
public final class AccountFactory {

    /** The bank's code in card numbers and IBANs. */
    public static final String BANK_CODE = "2370";
    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

    private final AccountRepository accounts;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public AccountFactory(AccountRepository accounts, Clock clock) {
        this.accounts = accounts;
        this.clock = clock;
    }

    public String newUsername() {
        return unique(AccountRepository.Identifier.USERNAME, () -> String.valueOf(100_000 + random.nextInt(900_000)));
    }

    /** A first password the customer is shown once: letters and digits, easy to read. */
    public String newPassword() {
        StringBuilder p = new StringBuilder();
        while (p.length() < 10) p.append(PASSWORD_CHARS.charAt(random.nextInt(PASSWORD_CHARS.length())));
        if (!p.toString().matches(".*\\d.*")) p.setCharAt(random.nextInt(10), (char) ('2' + random.nextInt(8)));
        if (!p.toString().matches(".*[A-Za-z].*")) p.setCharAt(random.nextInt(10), 'K');
        return p.toString();
    }

    /** A new account for a customer, waiting for review. */
    public Account newAccount(String username, String fullName, AccountType type, Money openingBalance) {
        String card = unique(AccountRepository.Identifier.CARD, () -> BANK_CODE + digits(12));
        String iban;
        String accountNumber;
        do {
            String bban = BANK_CODE + digits(16);
            iban = "IR" + checkDigits(bban) + bban;
            accountNumber = bban.substring(bban.length() - 10);
        } while (accounts.isTaken(AccountRepository.Identifier.IBAN, iban) || accounts.isTaken(AccountRepository.Identifier.ACCOUNT, accountNumber));
        String expiry = YearMonth.now(clock).plusYears(4).format(DateTimeFormatter.ofPattern("yyyy.MM"));
        return new Account(username, fullName, accountNumber, type, AccountStatus.PENDING, card, digits(3), iban, expiry, openingBalance);
    }

    /** The two check digits of an IBAN (ISO 13616, mod 97), so the numbers are valid IBANs. */
    static String checkDigits(String bban) {
        String rearranged = bban + "182700"; // "IR00" moved to the end, letters as numbers: I = 18, R = 27
        int mod = new BigInteger(rearranged).mod(BigInteger.valueOf(97)).intValue();
        return String.format("%02d", 98 - mod);
    }

    private String digits(int n) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < n; i++) b.append(random.nextInt(10));
        return b.toString();
    }

    private String unique(AccountRepository.Identifier kind, java.util.function.Supplier<String> make) {
        for (int tries = 0; tries < 100; tries++) {
            String candidate = make.get();
            if (!accounts.isTaken(kind, candidate)) return candidate;
        }
        throw new BankException("Could not create a new account number. Please try again.");
    }
}
