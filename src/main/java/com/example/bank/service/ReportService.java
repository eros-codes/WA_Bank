package com.example.bank.service;

import com.example.bank.model.BillPayment;
import com.example.bank.model.HistoryItem;
import com.example.bank.model.LedgerEntry;
import com.example.bank.model.TopUp;
import com.example.bank.repository.AccountRepository;
import com.example.bank.repository.LedgerRepository;
import com.example.bank.repository.LoanRepository;
import com.example.bank.repository.PaymentRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Read-only views: a customer's history and the numbers on the staff dashboard. */
public final class ReportService {

    private final AccountRepository accounts;
    private final LedgerRepository ledger;
    private final PaymentRepository payments;
    private final LoanRepository loans;

    public ReportService(AccountRepository accounts, LedgerRepository ledger, PaymentRepository payments, LoanRepository loans) {
        this.accounts = accounts;
        this.ledger = ledger;
        this.payments = payments;
        this.loans = loans;
    }

    /** Transfers in and out of any of the customer's accounts, then bills and top-ups. */
    public List<HistoryItem> historyOf(String username) {
        var owned = accounts.findByUsername(username);
        List<String> numbers = owned.stream().map(a -> a.accountNumber()).toList();
        List<String> cards = owned.stream().map(a -> a.cardNumber()).toList();
        List<String> ibans = owned.stream().map(a -> a.iban()).toList();
        List<HistoryItem> items = new ArrayList<>();
        for (LedgerEntry e : ledger.findForAccounts(numbers, cards, ibans)) {
            boolean outgoing = numbers.contains(e.sourceAccount()) || cards.contains(e.sourceCard());
            String title = outgoing ? e.kind() + " to " + e.destinationName() : e.kind() + " from " + e.sourceName();
            items.add(new HistoryItem(HistoryItem.Kind.TRANSFER, e.id(), title, outgoing ? e.amount().times(java.math.BigDecimal.ONE.negate()) : e.amount()));
        }
        for (BillPayment b : payments.findBills(username)) items.add(new HistoryItem(HistoryItem.Kind.BILL, b.transactionNo(), b.billType(), b.amount()));
        for (TopUp t : payments.findTopUps(username)) items.add(new HistoryItem(HistoryItem.Kind.TOP_UP, t.transactionNo(), "Charge to " + t.phoneNumber(), t.amount()));
        return items;
    }

    public Optional<LedgerEntry> transfer(int id) {
        return ledger.findById(id);
    }

    public Optional<BillPayment> bill(int no) {
        return payments.findBill(no);
    }

    public Optional<TopUp> topUp(int no) {
        return payments.findTopUp(no);
    }

    public List<LedgerEntry> allTransfers() {
        return ledger.findAll();
    }

    public List<BillPayment> allBills() {
        return payments.findAllBills();
    }

    public List<TopUp> allTopUps() {
        return payments.findAllTopUps();
    }

    public int transferCount() {
        return ledger.findAll().size();
    }

    public int billCount() {
        return payments.countBills();
    }

    public int loanRequestCount() {
        return loans.countAll();
    }
}
