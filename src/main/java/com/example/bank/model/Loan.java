package com.example.bank.model;

/** A loan request and where it is in review. The documents are loaded only when needed. */
public record Loan(long id, String username, String applicantName, Money amount, String term, String interest, String date, LoanStatus status) {
}
