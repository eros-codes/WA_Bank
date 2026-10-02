package com.example.bank.model;

/** Everything a sign-up produces: who the customer is, their first account, and the ID photo for review. */
public record NewAccountRequest(CustomerProfile profile, String password, Account account, byte[] idPhoto) {
}
