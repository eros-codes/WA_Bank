package com.example.bank.db;

import java.sql.ResultSet;
import java.sql.SQLException;

/** Turns the current row of a result set into an object. */
@FunctionalInterface
public interface RowMapper<T> {
    T map(ResultSet row) throws SQLException;
}
