package com.example.bank.service;

import com.example.bank.db.Database;
import com.example.bank.model.Account;
import com.example.bank.model.BankDirectory;
import com.example.bank.model.CardExpiry;
import com.example.bank.model.IbanMethod;
import com.example.bank.model.LedgerEntry;
import com.example.bank.model.Money;
import com.example.bank.model.Recipient;
import com.example.bank.repository.AccountRepository;
import com.example.bank.repository.BankFundsRepository;
import com.example.bank.repository.LedgerRepository;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Moving money. Every transfer is one database transaction: the debit
 * (amount plus fee, and only if the money is there at that moment), the
 * credit, the fee to the bank and the record happen together or not at all.
 *
 * Fees: a card transfer costs 7,200 IRR, always (the old code took it and,
 * for transfers inside the bank, it vanished). An account transfer is free.
 * An IBAN transfer inside the bank is free; to another bank it costs what
 * its method charges.
 */
public final class TransferService {

    public static final Money CARD_FEE = Money.of(7_200);

    private final Database db;
    private final AccountRepository accounts;
    private final LedgerRepository ledger;
    private final BankFundsRepository funds;
    private final Clock clock;

    public TransferService(Database db, AccountRepository accounts, LedgerRepository ledger, BankFundsRepository funds, Clock clock) {
        this.db = db;
        this.accounts = accounts;
        this.ledger = ledger;
        this.funds = funds;
        this.clock = clock;
    }

    /* ---- who would receive it: shown before the customer confirms ---- */

    public Recipient recipientByCard(String cardNumber) {
        String card = digits(cardNumber);
        if (card.length() != 16) throw new BankException("A card number has 16 digits.");
        var own = accounts.findByCardNumber(card);
        if (own.isPresent()) return new Recipient(own.get().fullName(), BankDirectory.OWN_BANK, own.get());
        String bank = BankDirectory.bankOfCard(card).orElseThrow(() -> new NotFoundException("This card is not from a bank we know."));
        return new Recipient("Unknown", bank, null);
    }

    public Recipient recipientByAccount(String accountNumber) {
        return accounts.findByAccountNumber(accountNumber.strip())
                .map(a -> new Recipient(a.fullName(), BankDirectory.OWN_BANK, a))
                .orElseThrow(() -> new NotFoundException("The destination person is not a member of our bank."));
    }

    public Recipient recipientByIban(String iban) {
        return findIban(iban)
                .map(a -> new Recipient(a.fullName(), BankDirectory.OWN_BANK, a))
                .orElse(new Recipient("Unknown", "Another bank", null));
    }

    /** IBANs are typed as digits only, and stored with or without "IR": both forms are tried. */
    private java.util.Optional<Account> findIban(String iban) {
        String s = normalizeIban(iban);
        String digits = s.startsWith("IR") ? s.substring(2) : s;
        return accounts.findByIban(digits).or(() -> accounts.findByIban("IR" + digits));
    }

    /** The customer's own card, if the details they typed are right. */
    public Account ownCard(String username, String card, String cvv, String expiry) {
        return accounts.findByCardNumber(digits(card))
                .filter(a -> a.username().equals(username) && a.cvv().equals(cvv.strip()) && CardExpiry.same(a.expiryDate(), expiry))
                .orElseThrow(() -> new BankException("The card details do not match your card."));
    }

    public Account ownAccount(String username, String accountNumber) {
        return accounts.findByAccountNumber(accountNumber.strip()).filter(a -> a.username().equals(username))
                .orElseThrow(() -> new NotFoundException("That source account is not yours."));
    }

    /* ---- the transfers ---- */

    public LedgerEntry cardToCard(String username, String sourceCard, String cvv, String expiry, String destinationCard, Money amount) {
        Account from = ownCard(username, sourceCard, cvv, expiry);
        return move("Card", from, recipientByCard(destinationCard), digits(destinationCard), null, amount, CARD_FEE);
    }

    public LedgerEntry accountToAccount(String username, String sourceAccount, String destinationAccount, Money amount) {
        Account from = ownAccount(username, sourceAccount);
        return move("Account", from, recipientByAccount(destinationAccount), null, null, amount, Money.ZERO);
    }

    /** IBAN transfer from the customer's own IBAN; the card's CVV and expiry confirm it is theirs. */
    public LedgerEntry ibanTransfer(String username, String sourceIban, String cvv, String expiry, String destinationIban, Money amount, IbanMethod method) {
        requirePositive(amount);
        if (method == null) throw new BankException("Choose a transfer method.");
        String refusal = method.refusal(amount);
        if (refusal != null) throw new BankException(refusal);
        Account from = findIban(sourceIban).filter(a -> a.username().equals(username) && a.cvv().equals(cvv.strip()) && CardExpiry.same(a.expiryDate(), expiry))
                .orElseThrow(() -> new BankException("The IBAN and card details do not match yours."));
        String iban = normalizeIban(destinationIban);
        if (!iban.matches("(IR)?\\d{24}")) throw new BankException("An IBAN has 24 digits.");
        Recipient to = recipientByIban(iban);
        return move("iBan", from, to, null, iban, amount, to.isOwnBank() ? Money.ZERO : method.fee(amount));
    }

    /** What a transfer would cost in total, for the confirmation screens. */
    public static Money totalCost(Money amount, Money fee) {
        return amount.plus(fee);
    }

    private LedgerEntry move(String kind, Account from, Recipient to, String destinationCard, String destinationIban, Money amount, Money fee) {
        requirePositive(amount);
        if (!from.isUsable()) throw new BankException("Your account has not been approved yet.");
        if (to.isOwnBank()) {
            if (!to.account().isUsable()) throw new BankException("The destination account cannot receive money.");
            if (from.accountNumber().equals(to.account().accountNumber())) throw new BankException("You cannot send money to the same account.");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        Money total = amount.plus(fee);
        return db.inTransaction(c -> {
            if (!accounts.debit(c, from.accountNumber(), total)) throw new InsufficientFundsException();
            if (to.isOwnBank() && !accounts.credit(c, to.account().accountNumber(), amount)) {
                throw new NotFoundException("The destination account no longer exists.");
            }
            if (fee.isPositive()) funds.add(c, fee);
            Account dest = to.account();
            LedgerEntry entry = new LedgerEntry(0, kind, from.fullName(), from.cardNumber(), from.accountNumber(), from.iban(), amount,
                    to.name(), dest != null ? dest.cardNumber() : destinationCard, dest != null ? dest.accountNumber() : null,
                    dest != null ? dest.iban() : destinationIban, to.bank(), now, fee);
            int id = ledger.record(c, entry);
            return new LedgerEntry(id, kind, entry.sourceName(), entry.sourceCard(), entry.sourceAccount(), entry.sourceIban(), amount,
                    entry.destinationName(), entry.destinationCard(), entry.destinationAccount(), entry.destinationIban(), entry.destinationBank(), now, fee);
        });
    }

    private static void requirePositive(Money amount) {
        // the old card form accepted "-1000000", which moved money from the recipient to the sender
        if (amount == null || !amount.isPositive()) throw new BankException("Enter an amount greater than zero.");
    }

    static String digits(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder();
        for (char ch : s.toCharArray()) {
            if (ch >= '0' && ch <= '9') b.append(ch);
            else if (ch >= '\u06F0' && ch <= '\u06F9') b.append((char) ('0' + (ch - '\u06F0')));
        }
        return b.toString();
    }

    static String normalizeIban(String iban) {
        return iban == null ? "" : iban.replace(" ", "").toUpperCase();
    }
}
