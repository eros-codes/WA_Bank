package com.example.bank.service;

import com.example.bank.db.Database;
import com.example.bank.model.Account;
import com.example.bank.model.BillPayment;
import com.example.bank.model.BillType;
import com.example.bank.model.CardExpiry;
import com.example.bank.model.MobileOperator;
import com.example.bank.model.Money;
import com.example.bank.model.TopUp;
import com.example.bank.repository.AccountRepository;
import com.example.bank.repository.BankFundsRepository;
import com.example.bank.repository.PaymentRepository;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Paying bills and buying phone credit. Like a payment gateway, any card of
 * the bank can pay if its details are right. Each payment carries a 9% tax,
 * which goes to the bank's funds in the same transaction as the payment.
 */
public final class PaymentService {

    public static final BigDecimal TAX = new BigDecimal("0.09");

    private final Database db;
    private final AccountRepository accounts;
    private final PaymentRepository payments;
    private final BankFundsRepository funds;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public PaymentService(Database db, AccountRepository accounts, PaymentRepository payments, BankFundsRepository funds, Clock clock) {
        this.db = db;
        this.accounts = accounts;
        this.payments = payments;
        this.funds = funds;
        this.clock = clock;
    }

    public static Money tax(Money amount) {
        return amount.times(TAX);
    }

    /** The account behind a card, if its details match and it may be used. */
    public Account cardFor(String cardNumber, String cvv, String expiry) {
        return accounts.findByCardNumber(TransferService.digits(cardNumber))
                .filter(a -> a.cvv().equals(cvv.strip()) && CardExpiry.same(a.expiryDate(), expiry) && a.isUsable())
                .orElseThrow(() -> new BankException("The card details are wrong."));
    }

    /** A new bill number that has not been paid before (bill numbers are unique). */
    public int newBillId() {
        for (int tries = 0; tries < 50; tries++) {
            int id = 100_000 + random.nextInt(900_000);
            if (!payments.billAlreadyPaid(id)) return id;
        }
        throw new BankException("Could not issue a bill number. Please try again.");
    }

    public BillPayment payBill(Account card, BillType type, int billId, String dueDate) {
        if (payments.billAlreadyPaid(billId)) throw new BankException("This bill has already been paid.");
        LocalDateTime now = LocalDateTime.now(clock);
        Money amount = type.amount();
        return db.inTransaction(c -> {
            if (!accounts.debit(c, card.accountNumber(), amount.plus(tax(amount)))) throw new InsufficientFundsException();
            funds.add(c, tax(amount));
            BillPayment bill = new BillPayment(0, card.username(), type.label(), billId, amount, card.fullName(), card.accountNumber(), now, dueDate);
            int no = payments.recordBill(c, bill, tax(amount));
            return new BillPayment(no, bill.username(), type.label(), billId, amount, bill.paidBy(), bill.accountNo(), now, dueDate);
        });
    }

    public TopUp topUp(Account card, String phoneNumber, Money amount) {
        if (amount == null || !amount.isPositive()) throw new BankException("Choose an amount of credit.");
        if (!phoneNumber.matches("09\\d{9}") || MobileOperator.of(phoneNumber).isEmpty()) throw new BankException("Enter a valid mobile number, like 09123456789.");
        LocalDateTime now = LocalDateTime.now(clock);
        return db.inTransaction(c -> {
            if (!accounts.debit(c, card.accountNumber(), amount.plus(tax(amount)))) throw new InsufficientFundsException();
            funds.add(c, tax(amount));
            TopUp t = new TopUp(0, card.username(), card.fullName(), card.accountNumber(), phoneNumber, amount, now);
            int no = payments.recordTopUp(c, t, tax(amount));
            return new TopUp(no, t.username(), t.payerName(), t.accountNo(), phoneNumber, amount, now);
        });
    }
}
