package com.example.bank.repository;

import com.example.bank.model.TermDeposit;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Fixed-term deposits (the deposit table). */
public interface DepositRepository {

    int create(Connection c, TermDeposit deposit, String accountNumber) throws SQLException;

    Optional<TermDeposit> findByNumber(int number);

    List<TermDeposit> findByUsername(String username);

    List<TermDeposit> findAll();

    /** Closes a deposit; false if it was already closed. */
    boolean delete(Connection c, int number) throws SQLException;
}
