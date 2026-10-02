package com.example.bank.model;

/**
 * One bank account and its card. In this schema a customer who opens a
 * second account gets a second row, so an account, not a customer, is the unit.
 */
public record Account(
        String username,
        String fullName,
        String accountNumber,
        AccountType type,
        AccountStatus status,
        String cardNumber,
        String cvv,
        String iban,
        String expiryDate,
        Money balance) {

    /** The card number grouped the way it is printed on a card. */
    public String formattedCardNumber() {
        return cardNumber == null ? "" : cardNumber.replaceAll("(\\d{4})(?=\\d)", "$1 ");
    }

    public boolean isUsable() {
        return status == AccountStatus.ACCEPTED;
    }
}
