package com.example.bank.model;

/** A new loan request with its four documents. */
public record LoanApplication(String username, Money amount, String term, String interest, String date,
                              byte[] idDocument, byte[] employmentDocument, byte[] collateralDocument, byte[] deedDocument) {
}
