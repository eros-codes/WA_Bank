package com.example.bank.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The one way the app talks to the database. Every statement is prepared,
 * every connection is closed, and every failure becomes a
 * {@link DataAccessException} with a readable message.
 */
public final class Database {

    private final ConnectionProvider connections;

    public Database(ConnectionProvider connections) {
        this.connections = connections;
    }

    /** Runs work on its own connection, outside a transaction. */
    public <T> T withConnection(SqlWork<T> work) {
        try (Connection c = connections.open()) {
            return work.run(c);
        } catch (SQLException e) {
            throw new DataAccessException("The database could not be reached or refused the request.", e);
        }
    }

    /**
     * Runs work inside one transaction: everything it does is committed
     * together, or, if anything fails, nothing is. Money moves only through here.
     */
    public <T> T inTransaction(SqlWork<T> work) {
        try (Connection c = connections.open()) {
            boolean autoCommit = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(autoCommit);
            }
        } catch (SQLException e) {
            throw new DataAccessException("The operation failed and nothing was changed.", e);
        }
    }

    public <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        return withConnection(c -> query(c, sql, mapper, params));
    }

    public <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... params) {
        return withConnection(c -> queryOne(c, sql, mapper, params));
    }

    public int update(String sql, Object... params) {
        return withConnection(c -> update(c, sql, params));
    }

    /* ---- the same, on a connection the caller holds (for transactions) ---- */

    public static <T> List<T> query(Connection c, String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        try (PreparedStatement st = prepare(c, sql, params); ResultSet rs = st.executeQuery()) {
            List<T> rows = new ArrayList<>();
            while (rs.next()) rows.add(mapper.map(rs));
            return rows;
        }
    }

    public static <T> Optional<T> queryOne(Connection c, String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        try (PreparedStatement st = prepare(c, sql, params); ResultSet rs = st.executeQuery()) {
            return rs.next() ? Optional.ofNullable(mapper.map(rs)) : Optional.empty();
        }
    }

    public static int update(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement st = prepare(c, sql, params)) {
            return st.executeUpdate();
        }
    }

    private static PreparedStatement prepare(Connection c, String sql, Object... params) throws SQLException {
        PreparedStatement st = c.prepareStatement(sql);
        for (int i = 0; i < params.length; i++) st.setObject(i + 1, params[i]);
        return st;
    }
}
