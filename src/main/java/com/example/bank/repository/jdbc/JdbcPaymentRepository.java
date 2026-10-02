package com.example.bank.repository.jdbc;

import com.example.bank.db.Database;
import com.example.bank.model.BillPayment;
import com.example.bank.model.Money;
import com.example.bank.model.TopUp;
import com.example.bank.repository.PaymentRepository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/** Bills paid and phone credit bought: the bill_payments and mobile_charges tables. */
public final class JdbcPaymentRepository implements PaymentRepository {

    private static final String BILLS = "SELECT b.payment_id, u.username, b.bill_type, b.bill_identifier, b.amount, b.paid_by, "
            + "a.account_number, b.paid_at, DATE_FORMAT(b.due_date, '%Y.%m.%d') AS due "
            + "FROM bill_payments b JOIN accounts a ON a.account_id = b.account_id JOIN users u ON u.user_id = a.user_id";
    private static final String TOP_UPS = "SELECT m.charge_id, u.username, u.full_name, a.account_number, m.phone_number, m.amount, m.charged_at "
            + "FROM mobile_charges m JOIN accounts a ON a.account_id = m.account_id JOIN users u ON u.user_id = a.user_id";

    private final Database db;

    public JdbcPaymentRepository(Database db) {
        this.db = db;
    }

    static BillPayment bill(ResultSet r) throws SQLException {
        Timestamp paid = r.getTimestamp("paid_at");
        String id = r.getString("bill_identifier");
        return new BillPayment(r.getInt("payment_id"), r.getString("username"), r.getString("bill_type"),
                id != null && id.matches("\\d{1,9}") ? Integer.parseInt(id) : 0, new Money(r.getBigDecimal("amount")), r.getString("paid_by"),
                r.getString("account_number"), paid == null ? null : paid.toLocalDateTime(), r.getString("due"));
    }

    static TopUp topUp(ResultSet r) throws SQLException {
        Timestamp t = r.getTimestamp("charged_at");
        return new TopUp(r.getInt("charge_id"), r.getString("username"), r.getString("full_name"), r.getString("account_number"),
                r.getString("phone_number"), new Money(r.getBigDecimal("amount")), t == null ? null : t.toLocalDateTime());
    }

    private static int insert(Connection c, String sql, Object... v) throws SQLException {
        try (PreparedStatement st = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < v.length; i++) st.setObject(i + 1, v[i]);
            st.executeUpdate();
            try (ResultSet keys = st.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        }
    }

    @Override
    public boolean billAlreadyPaid(int billId) {
        return db.queryOne("SELECT 1 FROM bill_payments WHERE bill_identifier = ?", r -> true, String.valueOf(billId)).isPresent();
    }

    @Override
    public int recordBill(Connection c, BillPayment b, Money tax) throws SQLException {
        Date due = JdbcAccountRepository.date(b.dueDate());
        return insert(c, "INSERT INTO bill_payments (account_id, bill_type, bill_identifier, amount, tax, paid_by, paid_at, due_date) "
                        + "VALUES ((SELECT account_id FROM accounts WHERE account_number = ?), ?, ?, ?, ?, ?, ?, ?)",
                b.accountNo(), b.billType(), String.valueOf(b.billId()), b.amount().amount(), tax.amount(), b.paidBy(),
                Timestamp.valueOf(b.paidAt()), due);
    }

    @Override
    public int recordTopUp(Connection c, TopUp t, Money tax) throws SQLException {
        return insert(c, "INSERT INTO mobile_charges (account_id, phone_number, amount, tax, charged_at) "
                        + "VALUES ((SELECT account_id FROM accounts WHERE account_number = ?), ?, ?, ?, ?)",
                t.accountNo(), t.phoneNumber(), t.amount().amount(), tax.amount(), Timestamp.valueOf(t.time()));
    }

    @Override
    public List<BillPayment> findBills(String username) {
        return db.query(BILLS + " WHERE u.username = ? ORDER BY b.paid_at DESC", JdbcPaymentRepository::bill, username);
    }

    @Override
    public List<TopUp> findTopUps(String username) {
        return db.query(TOP_UPS + " WHERE u.username = ? ORDER BY m.charged_at DESC", JdbcPaymentRepository::topUp, username);
    }

    @Override
    public Optional<BillPayment> findBill(int transactionNo) {
        return db.queryOne(BILLS + " WHERE b.payment_id = ?", JdbcPaymentRepository::bill, transactionNo);
    }

    @Override
    public Optional<TopUp> findTopUp(int transactionNo) {
        return db.queryOne(TOP_UPS + " WHERE m.charge_id = ?", JdbcPaymentRepository::topUp, transactionNo);
    }

    @Override
    public List<BillPayment> findAllBills() {
        return db.query(BILLS + " ORDER BY b.paid_at DESC", JdbcPaymentRepository::bill);
    }

    @Override
    public List<TopUp> findAllTopUps() {
        return db.query(TOP_UPS + " ORDER BY m.charged_at DESC", JdbcPaymentRepository::topUp);
    }

    @Override
    public int countBills() {
        return db.queryOne("SELECT COUNT(*) FROM bill_payments", r -> r.getInt(1)).orElse(0);
    }
}
