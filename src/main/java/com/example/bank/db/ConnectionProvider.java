package com.example.bank.db;

import java.sql.Connection;
import java.sql.SQLException;

/** Where connections come from. The app uses {@link DriverManagerConnectionProvider}; tests can use another. */
@FunctionalInterface
public interface ConnectionProvider {
    Connection open() throws SQLException;
}
