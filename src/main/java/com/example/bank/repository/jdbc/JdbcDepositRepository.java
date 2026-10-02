package com.example.bank.repository.jdbc;

import com.example.bank.db.Database;
import com.example.bank.model.Money;
import com.example.bank.model.TermDeposit;
import com.example.bank.repository.DepositRepository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Fixed-term deposits: the deposits table. Only open ones are listed; closing one keeps its record. */
public final class JdbcDepositRepository implements DepositRepository {

    private static final String SELECT = "SELECT d.deposit_id, u.username, u.full_name, d.type, d.amount, d.duration_months, d.interest_rate, "
            + "d.payout_type, d.currency, d.opened_at FROM deposits d JOIN accounts a ON a.account_id = d.account_id "
            + "JOIN users u ON u.user_id = a.user_id WHERE d.status = 'active'";

    private final Database db;

    public JdbcDepositRepository(Database db) {
        this.db = db;
    }

    static TermDeposit deposit(ResultSet r) throws SQLException {
        int months = r.getInt("duration_months");
        String duration = r.wasNull() ? null
                : months % 12 == 0 ? (months / 12 == 1 ? "1 year" : months / 12 + " years") : (months == 1 ? "1 month" : months + " months");
        Date opened = r.getDate("opened_at");
        return new TermDeposit(r.getInt("deposit_id"), r.getString("username"), r.getString("full_name"), r.getString("type"),
                new Money(r.getBigDecimal("amount")), duration, JdbcLoanRepository.percent(r.getBigDecimal("interest_rate")),
                r.getString("payout_type"), r.getString("currency"), opened == null ? null : opened.toLocalDate().toString());
    }

    @Override
    public int create(Connection c, TermDeposit d, String accountNumber) throws SQLException {
        int months = TermDeposit.months(d.duration());
        LocalDate opened = LocalDate.now();
        String sql = "INSERT INTO deposits (account_id, type, amount, duration_months, interest_rate, payout_type, currency, opened_at, maturity_date) "
                + "VALUES ((SELECT account_id FROM accounts WHERE account_number = ?), ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement st = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            Object[] v = {accountNumber, d.type(), d.amount().amount(), months == 0 ? null : months,
                    d.interest() == null ? BigDecimal.ZERO : new BigDecimal(d.interest().replace("%", "").strip()),
                    d.payment(), d.currency(), Date.valueOf(opened), months == 0 ? null : Date.valueOf(opened.plusMonths(months))};
            for (int i = 0; i < v.length; i++) st.setObject(i + 1, v[i]);
            st.executeUpdate();
            try (ResultSet keys = st.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        }
    }

    @Override
    public Optional<TermDeposit> findByNumber(int number) {
        return db.queryOne(SELECT + " AND d.deposit_id = ?", JdbcDepositRepository::deposit, number);
    }

    @Override
    public List<TermDeposit> findByUsername(String username) {
        return db.query(SELECT + " AND u.username = ? ORDER BY d.deposit_id DESC", JdbcDepositRepository::deposit, username);
    }

    @Override
    public List<TermDeposit> findAll() {
        return db.query(SELECT + " ORDER BY d.deposit_id DESC", JdbcDepositRepository::deposit);
    }

    @Override
    public boolean delete(Connection c, int number) throws SQLException {
        return Database.update(c, "UPDATE deposits SET status = 'closed' WHERE deposit_id = ? AND status = 'active'", number) == 1;
    }
}
