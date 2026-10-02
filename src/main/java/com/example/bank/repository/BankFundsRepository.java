package com.example.bank.repository;

import com.example.bank.model.Money;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * The bank's own money: fees come in, salaries go out. It used to live in
 * income.csv, outside any transaction, so a fee could be taken from a
 * customer and never arrive. Now it moves in the same transaction.
 */
public interface BankFundsRepository {

    Money balance();

    void add(Connection c, Money amount) throws SQLException;

    /** Takes money only if the bank has it; false otherwise. */
    boolean take(Connection c, Money amount) throws SQLException;
}
