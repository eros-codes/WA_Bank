package com.example.bank.app;

import com.example.bank.config.AppConfig;
import com.example.bank.config.Env;
import com.example.bank.db.Database;
import com.example.bank.db.DriverManagerConnectionProvider;
import com.example.bank.repository.AccountRepository;
import com.example.bank.repository.BankFundsRepository;
import com.example.bank.repository.EmployeeRepository;
import com.example.bank.repository.LedgerRepository;
import com.example.bank.repository.LoanRepository;
import com.example.bank.repository.PaymentRepository;
import com.example.bank.repository.jdbc.JdbcAccountRepository;
import com.example.bank.repository.jdbc.JdbcBankFundsRepository;
import com.example.bank.repository.jdbc.JdbcChequeRepository;
import com.example.bank.repository.jdbc.JdbcDepositRepository;
import com.example.bank.repository.jdbc.JdbcEmployeeRepository;
import com.example.bank.repository.jdbc.JdbcLedgerRepository;
import com.example.bank.repository.jdbc.JdbcLoanRepository;
import com.example.bank.repository.jdbc.JdbcPaymentRepository;
import com.example.bank.security.Pbkdf2PasswordHasher;
import com.example.bank.service.AccountFactory;
import com.example.bank.service.AuthService;
import com.example.bank.service.ChequeService;
import com.example.bank.service.CustomerService;
import com.example.bank.service.DepositService;
import com.example.bank.service.LoanService;
import com.example.bank.service.PaymentService;
import com.example.bank.service.ReportService;
import com.example.bank.service.StaffService;
import com.example.bank.service.TransferService;

import java.time.Clock;

/**
 * The composition root: the one place where the app's objects are created
 * and wired together. Controllers ask it for services; nothing else creates
 * repositories or connections.
 */
public final class AppContext {

    private static AppContext instance;

    private final AuthService auth;
    private final TransferService transfers;
    private final PaymentService payments;
    private final LoanService loans;
    private final ChequeService cheques;
    private final DepositService deposits;
    private final CustomerService customers;
    private final StaffService staff;
    private final ReportService reports;

    private AppContext(AppConfig config) {
        Clock clock = Clock.systemDefaultZone();
        Database db = new Database(new DriverManagerConnectionProvider(config));
        AccountRepository accountRepo = new JdbcAccountRepository(db);
        EmployeeRepository employeeRepo = new JdbcEmployeeRepository(db, "EMPLOYEE");
        EmployeeRepository managerRepo = new JdbcEmployeeRepository(db, "MANAGER");
        BankFundsRepository fundsRepo = new JdbcBankFundsRepository(db);
        LedgerRepository ledgerRepo = new JdbcLedgerRepository(db);
        LoanRepository loanRepo = new JdbcLoanRepository(db);
        PaymentRepository paymentRepo = new JdbcPaymentRepository(db);

        this.auth = new AuthService(accountRepo, employeeRepo, managerRepo, new Pbkdf2PasswordHasher(config.passwordHashIterations()));
        this.transfers = new TransferService(db, accountRepo, ledgerRepo, fundsRepo, clock);
        this.payments = new PaymentService(db, accountRepo, paymentRepo, fundsRepo, clock);
        this.loans = new LoanService(db, loanRepo, accountRepo);
        this.cheques = new ChequeService(new JdbcChequeRepository(db), accountRepo, clock);
        this.deposits = new DepositService(db, new JdbcDepositRepository(db), accountRepo);
        this.customers = new CustomerService(db, accountRepo, auth, new AccountFactory(accountRepo, clock));
        this.staff = new StaffService(db, employeeRepo, accountRepo, fundsRepo);
        this.reports = new ReportService(accountRepo, ledgerRepo, paymentRepo, loanRepo);
    }

    /** Created on first use from the environment and the .env file. */
    public static synchronized AppContext get() {
        if (instance == null) instance = new AppContext(AppConfig.fromEnv(Env.load()));
        return instance;
    }

    public AuthService auth() { return auth; }
    public TransferService transfers() { return transfers; }
    public PaymentService payments() { return payments; }
    public LoanService loans() { return loans; }
    public ChequeService cheques() { return cheques; }
    public DepositService deposits() { return deposits; }
    public CustomerService customers() { return customers; }
    public StaffService staff() { return staff; }
    public ReportService reports() { return reports; }
}
