package com.example.bank.repository.jdbc;

import com.example.bank.db.Database;
import com.example.bank.model.Loan;
import com.example.bank.model.LoanApplication;
import com.example.bank.model.LoanStatus;
import com.example.bank.model.Money;
import com.example.bank.model.TermDeposit;
import com.example.bank.repository.LoanRepository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Loans and their documents: the loans and loan_documents tables. */
public final class JdbcLoanRepository implements LoanRepository {

    private static final String SELECT = "SELECT l.loan_id, u.username, u.full_name, l.loan_amount, l.term_months, l.interest_rate, "
            + "DATE_FORMAT(l.request_date, '%Y.%m.%d') AS requested, l.status FROM loans l JOIN users u ON u.user_id = l.user_id";

    private final Database db;

    public JdbcLoanRepository(Database db) {
        this.db = db;
    }

    static Loan loan(ResultSet r) throws SQLException {
        int months = r.getInt("term_months");
        String term = months % 12 == 0 ? (months / 12 == 1 ? "1 year" : months / 12 + " years") : months + " months";
        return new Loan(r.getLong("loan_id"), r.getString("username"), r.getString("full_name"), new Money(r.getBigDecimal("loan_amount")),
                term, percent(r.getBigDecimal("interest_rate")), r.getString("requested"), LoanStatus.fromDb(r.getString("status")));
    }

    static String percent(BigDecimal rate) {
        return rate == null ? null : rate.stripTrailingZeros().toPlainString() + "%";
    }

    /** The loan and its four documents are saved together. */
    @Override
    public long create(LoanApplication a) {
        return db.inTransaction(c -> {
            long id;
            String sql = "INSERT INTO loans (user_id, loan_amount, term_months, interest_rate, request_date, status) "
                    + "VALUES ((SELECT user_id FROM users WHERE username = ?), ?, ?, ?, ?, ?)";
            try (PreparedStatement st = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                Object[] v = {a.username(), a.amount().amount(), TermDeposit.months(a.term()), rate(a.interest()), requestDate(a.date()),
                        LoanStatus.PENDING.dbValue()};
                for (int i = 0; i < v.length; i++) st.setObject(i + 1, v[i]);
                st.executeUpdate();
                try (ResultSet keys = st.getGeneratedKeys()) {
                    keys.next();
                    id = keys.getLong(1);
                }
            }
            document(c, id, "id", a.idDocument());
            document(c, id, "employment", a.employmentDocument());
            document(c, id, "collateral", a.collateralDocument());
            document(c, id, "deed", a.deedDocument());
            return id;
        });
    }

    private static void document(Connection c, long loanId, String type, byte[] content) throws SQLException {
        if (content != null) Database.update(c, "INSERT INTO loan_documents (loan_id, document_type, content) VALUES (?, ?, ?)", loanId, type, content);
    }

    private static BigDecimal rate(String interest) {
        return interest == null ? BigDecimal.ZERO : new BigDecimal(interest.replace("%", "").strip());
    }

    private static Date requestDate(String text) {
        Date d = JdbcAccountRepository.date(text);
        return d != null ? d : Date.valueOf(LocalDate.now());
    }

    private static String in(Set<LoanStatus> statuses) {
        return "(" + String.join(", ", Collections.nCopies(statuses.size(), "?")) + ")";
    }

    private static Object[] values(Set<LoanStatus> statuses) {
        return statuses.stream().map(LoanStatus::dbValue).toArray();
    }

    @Override
    public List<Loan> findByStatus(Set<LoanStatus> statuses) {
        return db.query(SELECT + " WHERE l.status IN " + in(statuses) + " ORDER BY l.loan_id", JdbcLoanRepository::loan, values(statuses));
    }

    @Override
    public Optional<Loan> findById(long id) {
        return db.queryOne(SELECT + " WHERE l.loan_id = ?", JdbcLoanRepository::loan, id);
    }

    @Override
    public int count(Set<LoanStatus> statuses) {
        return db.queryOne("SELECT COUNT(*) FROM loans WHERE status IN " + in(statuses), r -> r.getInt(1), values(statuses)).orElse(0);
    }

    @Override
    public int countAll() {
        return db.queryOne("SELECT COUNT(*) FROM loans", r -> r.getInt(1)).orElse(0);
    }

    @Override
    public boolean changeStatus(Connection c, long id, LoanStatus from, LoanStatus to) throws SQLException {
        return Database.update(c, "UPDATE loans SET status = ? WHERE loan_id = ? AND status = ?", to.dbValue(), id, from.dbValue()) == 1;
    }
}
