package com.example.bank.repository;

import com.example.bank.model.Cheque;
import com.example.bank.model.ChequeStatus;

import java.util.List;

/** Cheques (the check table). */
public interface ChequeRepository {

    /** Records a cheque drawn on an account and returns its number. */
    int create(Cheque cheque, String issuerAccountNumber);

    /** Cheques written by a customer: by who they are, not by their name, which others may share. */
    List<Cheque> findByIssuer(String username);

    List<Cheque> findPending();

    List<Cheque> findReviewed();

    /** Records a review; false if the cheque had already been reviewed. */
    boolean review(int number, ChequeStatus decision);
}
