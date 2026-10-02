import com.example.bank.db.*;
import com.example.bank.model.*;
import com.example.bank.repository.jdbc.*;
import com.example.bank.security.Pbkdf2PasswordHasher;
import com.example.bank.service.*;

import java.sql.DriverManager;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Runs the new core against a real, migrated database. */
public class CoreTest {
    static int failures = 0;
    static void check(boolean ok, String what) { System.out.println((ok ? "  PASS  " : "  FAIL  ") + what); if (!ok) failures++; }

    public static void main(String[] a) throws Exception {
        Database db = new Database(() -> DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/mig", "root", ""));
        var accounts = new JdbcAccountRepository(db); var employees = new JdbcEmployeeRepository(db, "EMPLOYEE");
        var ledger = new JdbcLedgerRepository(db); var loans = new JdbcLoanRepository(db); var payments = new JdbcPaymentRepository(db);
        var auth = new AuthService(accounts, employees, new JdbcEmployeeRepository(db, "MANAGER"), new Pbkdf2PasswordHasher(10_000));
        var transfers = new TransferService(db, accounts, ledger, new JdbcBankFundsRepository(db), Clock.systemDefaultZone());
        var loanService = new LoanService(db, loans, accounts);
        var customers = new CustomerService(db, accounts, auth, new AccountFactory(accounts, Clock.systemDefaultZone()));
        var reports = new ReportService(accounts, ledger, payments, loans);
        java.util.function.Supplier<Money> sara = () -> accounts.findByAccountNumber("1000000001").get().balance();
        java.util.function.Supplier<Money> ali = () -> accounts.findByAccountNumber("1000000002").get().balance();

        System.out.println("1. old plain-text password");
        check(auth.loginCustomer("1001", "pass1234").isPresent(), "accepted once");
        check(accounts.passwordHashOf("1001").startsWith("pbkdf2$"), "and hashed at that moment");
        check(auth.loginCustomer("1001", "pass1234").isPresent(), "still signs in with the hash");
        check(auth.loginCustomer("1001", "wrong").isEmpty(), "a wrong password is refused");
        check(auth.loginEmployee("emp1", "worker123").isPresent() && employees.passwordHashOf("emp1").startsWith("pbkdf2$"), "staff too");

        System.out.println("2. a transfer");
        Money s0 = sara.get(), a0 = ali.get();
        var entry = transfers.cardToCard("1001", "6037 9900 0000 0001", "123", "2030.05", "6037990000000002", Money.of(1_000_000));
        check(sara.get().equals(s0.minus(Money.of(1_007_200))) && ali.get().equals(a0.plus(Money.of(1_000_000))), "sender pays 1,000,000 + 7,200 fee, receiver gets 1,000,000");
        check(entry.id() > 0 && reports.transfer(entry.id()).isPresent(), "and it is recorded");

        var funds = new JdbcBankFundsRepository(db);
        check(funds.balance().equals(Money.of(5_827_500 + 7_200)), "the fee reached the bank: " + funds.balance());

        System.out.println("2b. the negative-amount attack");
        Money s2 = sara.get(), a2 = ali.get();
        try { transfers.cardToCard("1001", "6037990000000001", "123", "2030.05", "6037990000000002", Money.parse("-1000000")); check(false, "refused"); }
        catch (BankException e) { check(true, "refused: " + e.getMessage()); }
        check(sara.get().equals(s2) && ali.get().equals(a2), "no money moved");

        System.out.println("2c. to a card at another bank");
        Money s3 = sara.get(), f3 = funds.balance();
        var ext = transfers.cardToCard("1001", "6037990000000001", "123", "2030.05", "6104330000001234", Money.of(200_000));
        check(sara.get().equals(s3.minus(Money.of(207_200))) && funds.balance().equals(f3.plus(Money.of(7_200))), "amount leaves, fee stays with the bank");
        check("Mellat Bank".equals(ext.destinationBank()), "recorded as going to Mellat Bank");

        System.out.println("2d. IBAN methods");
        try { transfers.ibanTransfer("1001", "000000000000000000000001", "123", "2030.05", "IR000000000000000000000002", Money.of(1_000_000), com.example.bank.model.IbanMethod.SATNA); check(false, "satna minimum"); }
        catch (BankException e) { check(true, "Satna's minimum is enforced now: " + e.getMessage()); }
        Money s4 = sara.get(), a4 = ali.get();
        transfers.ibanTransfer("1001", "000000000000000000000001", "123", "2030.05", "IR000000000000000000000002", Money.of(100_000), com.example.bank.model.IbanMethod.PAYA);
        check(sara.get().equals(s4.minus(Money.of(100_000))) && ali.get().equals(a4.plus(Money.of(100_000))), "inside the bank: no fee");
        Money s5 = sara.get(), f5 = funds.balance();
        transfers.ibanTransfer("1001", "000000000000000000000001", "123", "2030.05", "IR990000000000000000000099", Money.of(100_000), com.example.bank.model.IbanMethod.PAYA);
        check(sara.get().equals(s5.minus(Money.of(101_000))) && funds.balance().equals(f5.plus(Money.of(1_000))), "to another bank: 1% Paya fee to the bank");
        try { transfers.ibanTransfer("1001", "000000000000000000000002", "456", "2030.06", "IR990000000000000000000099", Money.of(100), com.example.bank.model.IbanMethod.PAYA); check(false, "someone else's card"); }
        catch (BankException e) { check(true, "cannot send from someone else's card"); }

        System.out.println("3. not enough money");
        Money s1 = sara.get(), a1 = ali.get(); int count1 = reports.transferCount();
        try { transfers.cardToCard("1001", "6037990000000001", "123", "2030.05", "6037990000000002", Money.of(999_000_000)); check(false, "refused"); }
        catch (InsufficientFundsException e) { check(true, "refused with: " + e.getMessage()); }
        check(sara.get().equals(s1) && ali.get().equals(a1) && reports.transferCount() == count1, "nothing changed, nothing recorded");
        try { transfers.cardToCard("1001", "6037990000000001", "999", "2030.05", "6037990000000002", Money.of(10)); check(false, "wrong CVV refused"); }
        catch (BankException e) { check(true, "a wrong CVV is refused"); }

        System.out.println("4. twenty withdrawals at the same moment");
        Money before = sara.get(); Money each = Money.of(1_000_000);
        ExecutorService pool = Executors.newFixedThreadPool(20); AtomicInteger ok = new AtomicInteger(), poor = new AtomicInteger();
        List<Future<?>> fs = new ArrayList<>();
        for (int i = 0; i < 20; i++) fs.add(pool.submit(() -> {
            try { transfers.accountToAccount("1001", "1000000001", "1000000002", each); ok.incrementAndGet(); }
            catch (InsufficientFundsException e) { poor.incrementAndGet(); }
        }));
        for (var f : fs) f.get(); pool.shutdown();
        Money expected = before.minus(each.times(new java.math.BigDecimal(ok.get())));
        check(!sara.get().isNegative(), "the balance never went below zero (" + ok.get() + " succeeded, " + poor.get() + " refused)");
        check(sara.get().equals(expected), "not a rial lost: " + sara.get() + " = " + before + " - " + ok.get() + " x 1,000,000");

        System.out.println("5. two loans of the same amount");
        loanService.employeeReview(1, true);
        loanService.managerDecision(1, true);
        check(loans.findById(1).get().status() == LoanStatus.ACCEPTED, "loan 1 accepted");
        check(loans.findById(2).get().status() == LoanStatus.PENDING, "loan 2, same amount, untouched");
        Money saving = accounts.findByAccountNumber("1000000003").get().balance();
        check(saving.equals(Money.of(2_500_000)), "the saving account was not paid");
        try { loanService.managerDecision(1, true); check(false, "cannot be paid twice"); } catch (BankException e) { check(true, "cannot be paid twice"); }

        System.out.println("6. history");
        var aliHistory = reports.historyOf("1002");
        check(aliHistory.stream().anyMatch(h -> h.title().contains("from Sara Ahmadi") && h.amount().isPositive()), "money received now shows up");

        System.out.println("7. the manager's profile edit");
        var p = accounts.findProfile("1002").get();
        customers.updateProfile(new CustomerProfile(p.username(), p.fullName(), p.nationalId(), p.birthDate(), p.gender(), p.maritalStatus(),
                p.nationality(), "Tehran", p.postalCode(), "09129999999", p.email(), "Doctor", p.employerAddress(), p.annualIncome(), p.creationDate()));
        var q = accounts.findProfile("1002").get();
        check(q.address().equals("Tehran") && q.phoneNumber().equals("09129999999") && q.occupation().equals("Doctor"), "works (it always failed before)");

        System.out.println(failures == 0 ? "ALL PASSED" : failures + " FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }
}
