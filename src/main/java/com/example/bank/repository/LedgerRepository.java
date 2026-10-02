package com.example.bank.repository;

import com.example.bank.model.LedgerEntry;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** The record of every transfer (the transitions table). */
public interface LedgerRepository {

    /** Records a transfer inside the caller's transaction and returns its id. */
    int record(Connection c, LedgerEntry entry) throws SQLException;

    Optional<LedgerEntry> findById(int id);

    /** Transfers into or out of any of these accounts, newest first. */
    List<LedgerEntry> findForAccounts(List<String> accountNumbers, List<String> cardNumbers, List<String> ibans);

    List<LedgerEntry> findAll();
}
