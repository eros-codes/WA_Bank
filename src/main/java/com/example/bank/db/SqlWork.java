package com.example.bank.db;

import java.sql.Connection;
import java.sql.SQLException;

/** A piece of work done with one connection, possibly inside a transaction. */
@FunctionalInterface
public interface SqlWork<T> {
    T run(Connection connection) throws SQLException;
}
