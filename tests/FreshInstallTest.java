import com.example.bank.db.Database;
import com.example.bank.model.*;
import com.example.bank.repository.jdbc.*;
import com.example.bank.security.Pbkdf2PasswordHasher;
import com.example.bank.service.*;

import java.sql.DriverManager;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A new installation, from database/schema.sql and nothing else: the first
 * manager added by hand as the README says, then two customers who sign up
 * and use the bank, and one who becomes an employee.
 * Connects to jdbc:mysql://127.0.0.1:3306/fresh as root; change it below.
 */
public class FreshInstallTest {
    static int failures = 0;
    static void check(boolean ok, String what) { System.out.println((ok ? "  PASS  " : "  FAIL  ") + what); if (!ok) failures++; }

    public static void main(String[] args) throws Exception {
        Database db = new Database(() -> DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/fresh", "root", ""));
        var accounts = new JdbcAccountRepository(db); var funds = new JdbcBankFundsRepository(db);
        var employees = new JdbcEmployeeRepository(db, "EMPLOYEE"); var managers = new JdbcEmployeeRepository(db, "MANAGER");
        var ledger = new JdbcLedgerRepository(db); var loans = new JdbcLoanRepository(db); var payments = new JdbcPaymentRepository(db);
        Clock clock = Clock.systemDefaultZone();
        var auth = new AuthService(accounts, employees, managers, new Pbkdf2PasswordHasher(10_000));
        var customers = new CustomerService(db, accounts, auth, new AccountFactory(accounts, clock));
        var transfers = new TransferService(db, accounts, ledger, funds, clock);
        var pay = new PaymentService(db, accounts, payments, funds, clock);
        var deposits = new DepositService(db, new JdbcDepositRepository(db), accounts);
        var cheques = new ChequeService(new JdbcChequeRepository(db), accounts, clock);
        var loanService = new LoanService(db, loans, accounts);
        var staff = new StaffService(db, employees, accounts, funds);
        var reports = new ReportService(accounts, ledger, payments, loans);

        System.out.println("1. the first manager, added with the README's SQL");
        db.update("INSERT INTO users (username, password_hash, full_name) VALUES ('manager', 'ChooseAStrong1', 'Maryam Boss')");
        db.update("INSERT INTO user_roles (user_id, role_id) SELECT u.user_id, r.role_id FROM users u, roles r WHERE u.username = 'manager' AND r.role_name = 'MANAGER'");
        check(auth.loginManager("manager", "ChooseAStrong1").isPresent(), "the manager signs in");
        check(managers.passwordHashOf("manager").startsWith("pbkdf2$"), "and the typed password was replaced by a hash");
        check(auth.loginEmployee("manager", "ChooseAStrong1").isEmpty(), "a manager is not an employee");

        System.out.println("2. two customers sign up");
        var sara = customers.signUp(profile("Sara Ahmadi", "0499370899", "09121111111", "sara@x.ir"), AccountType.CHECKING, Money.of(20_000_000), new byte[]{1});
        var ali = customers.signUp(profile("Ali Rezaei", "0084575948", "09122222222", "ali@x.ir"), AccountType.CHECKING, Money.of(1_000_000), new byte[]{1});
        check(auth.loginCustomer(sara.username(), sara.password()).map(l -> !l.approved()).orElse(false), "before review she can sign in but is not approved");
        var pending = customers.nextPending().orElseThrow();
        check(pending.idPhoto() != null && pending.idPhoto().length == 1, "the employee sees her ID photo");
        customers.approve(sara.account().accountNumber());
        customers.approve(ali.account().accountNumber());
        check(auth.loginCustomer(sara.username(), sara.password()).map(AuthService.CustomerLogin::approved).orElse(false), "after review she is in");
        try { customers.signUp(profile("Someone Else", "0499370899", "09123333333", "x@x.ir"), AccountType.CHECKING, Money.of(100_000), new byte[]{1}); check(false, "same national ID"); }
        catch (BankException e) { check(true, "a second sign-up with her national ID is refused"); }

        System.out.println("3. money moves");
        Account s = accounts.findPrimary(sara.username()).orElseThrow();
        var t = transfers.cardToCard(sara.username(), s.cardNumber(), s.cvv(), s.expiryDate(), ali.account().cardNumber(), Money.of(500_000));
        check(balance(accounts, s) .equals(Money.of(20_000_000 - 507_200)), "card to card: 500,000 + 7,200 fee");
        check(reports.transfer(t.id()).map(e -> e.fee().equals(Money.of(7_200)) && "Ali Rezaei".equals(e.destinationName())).orElse(false), "the record has the fee and the recipient");
        transfers.cardToCard(sara.username(), s.cardNumber(), s.cvv(), s.expiryDate(), "6104330000009999", Money.of(100_000));
        check(funds.balance().equals(Money.of(14_400)), "both fees reached the bank: " + funds.balance());
        var bill = pay.payBill(pay.cardFor(s.cardNumber(), s.cvv(), s.expiryDate()), BillType.GAS, pay.newBillId(), "2030.12.31");
        try { pay.payBill(pay.cardFor(s.cardNumber(), s.cvv(), s.expiryDate()), BillType.GAS, bill.billId(), "2030.12.31"); check(false, "paid twice"); }
        catch (BankException e) { check(true, "the same bill cannot be paid twice"); }
        var dep = deposits.open(sara.username(), s.accountNumber(), new TermDeposit(0, sara.username(), null, "Long term", Money.of(1_000_000), "2 years", "12%", "12 months", "IRR", null));
        check(deposits.of(sara.username()).size() == 1 && "2 years".equals(deposits.find(sara.username(), dep.number()).duration()), "a deposit is open: 2 years");
        deposits.close(sara.username(), dep.number());
        check(deposits.of(sara.username()).isEmpty(), "and closed again, its money back");
        int no = cheques.write(sara.username(), "Ali Rezaei", Money.of(50_000), LocalDateTime.now().plusDays(10));
        check(cheques.writtenBy(sara.username()).stream().anyMatch(c -> c.number() == no), "her cheque is listed under her");

        System.out.println("4. a loan, an employee and the manager");
        long loanId = loanService.apply(new LoanApplication(ali.username(), Money.of(3_000_000), "3 years", "18%", LocalDate.now().toString().replace('-', '.'),
                new byte[]{1}, new byte[]{2}, new byte[]{3}, new byte[]{4}));
        check(loans.findById(loanId).map(l -> "3 years".equals(l.term()) && "18%".equals(l.interest())).orElse(false), "the loan reads back as 3 years at 18%");
        staff.promote(sara.username());
        check(auth.loginEmployee(sara.username(), sara.password()).isPresent(), "Sara, promoted, signs in to the employee app with her own password");
        loanService.employeeReview(loanId, true);
        Money before = balance(accounts, accounts.findPrimary(ali.username()).orElseThrow());
        loanService.managerDecision(loanId, true);
        check(balance(accounts, accounts.findPrimary(ali.username()).orElseThrow()).equals(before.plus(Money.of(3_000_000))), "the accepted loan was paid to Ali");
        staff.remove(sara.username());
        check(auth.loginEmployee(sara.username(), sara.password()).isEmpty() && auth.loginCustomer(sara.username(), sara.password()).isPresent(),
                "removed from staff, she is still a customer");

        System.out.println("5. closing");
        try { customers.closeAccount(ali.username(), ali.account().accountNumber()); check(false, "closed with money"); }
        catch (BankException e) { check(true, "an account with money in it cannot be closed"); }
        System.out.println(failures == 0 ? "ALL PASSED" : failures + " FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    static CustomerProfile profile(String name, String nationalId, String phone, String email) {
        return new CustomerProfile(null, name, nationalId, "1998.4.12", "female", "single", "Iranian", "Tabriz-1-2-3-4-5", "5100000000",
                phone, email, "Engineer", "Tabriz", "500000000", null);
    }

    static Money balance(JdbcAccountRepository accounts, Account a) {
        return accounts.findByAccountNumber(a.accountNumber()).orElseThrow().balance();
    }
}
