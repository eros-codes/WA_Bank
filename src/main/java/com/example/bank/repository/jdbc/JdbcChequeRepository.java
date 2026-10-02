package com.example.bank.repository.jdbc;

import com.example.bank.db.Database;
import com.example.bank.model.Cheque;
import com.example.bank.model.ChequeStatus;
import com.example.bank.model.Money;
import com.example.bank.repository.ChequeRepository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/** Cheques: the checks table, with the writer's name from their account. */
public final class JdbcChequeRepository implements ChequeRepository {

    private static final String SELECT = "SELECT k.check_id, u.full_name, k.amount, k.recipient_name, k.issue_date, k.due_date, k.status "
            + "FROM checks k JOIN accounts a ON a.account_id = k.issuer_account_id JOIN users u ON u.user_id = a.user_id";

    private final Database db;

    public JdbcChequeRepository(Database db) {
        this.db = db;
    }

    static Cheque cheque(ResultSet r) throws SQLException {
        Date issued = r.getDate("issue_date"), due = r.getDate("due_date");
        return new Cheque(r.getInt("check_id"), r.getString("full_name"), new Money(r.getBigDecimal("amount")), r.getString("recipient_name"),
                issued == null ? null : issued.toLocalDate().atStartOfDay(), due == null ? null : due.toLocalDate().atStartOfDay(),
                ChequeStatus.fromDb(r.getString("status")));
    }

    @Override
    public int create(Cheque c, String issuerAccountNumber) {
        return db.withConnection(conn -> {
            String sql = "INSERT INTO checks (issuer_account_id, recipient_name, amount, issue_date, due_date, status) "
                    + "VALUES ((SELECT account_id FROM accounts WHERE account_number = ?), ?, ?, ?, ?, ?)";
            try (PreparedStatement st = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                Object[] v = {issuerAccountNumber, c.recipientName(), c.amount().amount(), Date.valueOf(c.date().toLocalDate()),
                        Date.valueOf(c.dueDate().toLocalDate()), ChequeStatus.PENDING.dbValue()};
                for (int i = 0; i < v.length; i++) st.setObject(i + 1, v[i]);
                st.executeUpdate();
                try (ResultSet keys = st.getGeneratedKeys()) {
                    return keys.next() ? keys.getInt(1) : 0;
                }
            }
        });
    }

    @Override
    public List<Cheque> findByIssuer(String username) {
        return db.query(SELECT + " WHERE u.username = ? ORDER BY k.check_id DESC", JdbcChequeRepository::cheque, username);
    }

    @Override
    public List<Cheque> findPending() {
        return db.query(SELECT + " WHERE k.status = ? ORDER BY k.check_id", JdbcChequeRepository::cheque, ChequeStatus.PENDING.dbValue());
    }

    @Override
    public List<Cheque> findReviewed() {
        return db.query(SELECT + " WHERE k.status <> ? ORDER BY k.check_id DESC", JdbcChequeRepository::cheque, ChequeStatus.PENDING.dbValue());
    }

    @Override
    public boolean review(int number, ChequeStatus decision) {
        return db.update("UPDATE checks SET status = ? WHERE check_id = ? AND status = ?", decision.dbValue(), number, ChequeStatus.PENDING.dbValue()) == 1;
    }
}
