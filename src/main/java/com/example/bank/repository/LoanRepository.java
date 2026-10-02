package com.example.bank.repository;

import com.example.bank.model.Loan;
import com.example.bank.model.LoanApplication;
import com.example.bank.model.LoanStatus;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Loan requests (the loan table). A loan is found by its own id, never by owner and amount. */
public interface LoanRepository {

    long create(LoanApplication application);

    List<Loan> findByStatus(Set<LoanStatus> statuses);

    Optional<Loan> findById(long id);

    int count(Set<LoanStatus> statuses);

    int countAll();

    /** Moves a loan from one status to another; false if it was not in the expected status any more. */
    boolean changeStatus(Connection c, long id, LoanStatus from, LoanStatus to) throws SQLException;
}
