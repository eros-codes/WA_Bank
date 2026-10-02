package com.example.bank.repository.jdbc;

import com.example.bank.db.Database;
import com.example.bank.model.LedgerEntry;
import com.example.bank.model.Money;
import com.example.bank.repository.LedgerRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Transfers: the transactions table, with both sides joined to their accounts and owners. */
public final class JdbcLedgerRepository implements LedgerRepository {

    private static final String SELECT = "SELECT t.transaction_id, t.transaction_type, t.amount, t.fee, t.created_at, "
            + "su.full_name AS source_name, sa.card_number AS source_card, sa.account_number AS source_account, sa.iban AS source_iban, "
            + "COALESCE(du.full_name, t.destination_name) AS destination_name, COALESCE(da.card_number, t.destination_card) AS destination_card, "
            + "da.account_number AS destination_account, COALESCE(da.iban, t.destination_iban) AS destination_iban, t.destination_bank_name "
            + "FROM transactions t "
            + "LEFT JOIN accounts sa ON sa.account_id = t.source_account_id LEFT JOIN users su ON su.user_id = sa.user_id "
            + "LEFT JOIN accounts da ON da.account_id = t.destination_account_id LEFT JOIN users du ON du.user_id = da.user_id";

    private final Database db;

    public JdbcLedgerRepository(Database db) {
        this.db = db;
    }

    static LedgerEntry entry(ResultSet r) throws SQLException {
        Timestamp t = r.getTimestamp("created_at");
        return new LedgerEntry(r.getInt("transaction_id"), r.getString("transaction_type"), r.getString("source_name"),
                r.getString("source_card"), r.getString("source_account"), r.getString("source_iban"),
                new Money(r.getBigDecimal("amount")), r.getString("destination_name"), r.getString("destination_card"),
                r.getString("destination_account"), r.getString("destination_iban"), r.getString("destination_bank_name"),
                t == null ? null : t.toLocalDateTime(), new Money(r.getBigDecimal("fee")));
    }

    /** Records a transfer. A destination outside the bank has no account here; its details are kept as given. */
    @Override
    public int record(Connection c, LedgerEntry e) throws SQLException {
        String sql = "INSERT INTO transactions (source_account_id, destination_account_id, transaction_type, amount, fee, "
                + "destination_name, destination_card, destination_iban, destination_bank_name, created_at) VALUES ("
                + "(SELECT account_id FROM accounts WHERE account_number = ?), (SELECT account_id FROM accounts WHERE account_number = ?), "
                + "?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement st = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            Object[] v = {e.sourceAccount(), e.destinationAccount(), e.kind(), e.amount().amount(),
                    e.fee() == null ? Money.ZERO.amount() : e.fee().amount(), e.destinationName(), e.destinationCard(), e.destinationIban(),
                    e.destinationBank(), Timestamp.valueOf(e.time())};
            for (int i = 0; i < v.length; i++) st.setObject(i + 1, v[i]);
            st.executeUpdate();
            try (ResultSet keys = st.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        }
    }

    @Override
    public Optional<LedgerEntry> findById(int id) {
        return db.queryOne(SELECT + " WHERE t.transaction_id = ?", JdbcLedgerRepository::entry, id);
    }

    @Override
    public List<LedgerEntry> findForAccounts(List<String> accounts, List<String> cards, List<String> ibans) {
        List<String> present = accounts.stream().filter(a -> a != null && !a.isBlank()).toList();
        if (present.isEmpty()) return Collections.emptyList();
        String in = String.join(", ", Collections.nCopies(present.size(), "?"));
        List<Object> params = new ArrayList<>(present);
        params.addAll(present);
        return db.query(SELECT + " WHERE sa.account_number IN (" + in + ") OR da.account_number IN (" + in + ") "
                + "ORDER BY t.created_at DESC, t.transaction_id DESC", JdbcLedgerRepository::entry, params.toArray());
    }

    @Override
    public List<LedgerEntry> findAll() {
        return db.query(SELECT + " ORDER BY t.created_at DESC, t.transaction_id DESC", JdbcLedgerRepository::entry);
    }
}
