package com.example.bank.model;

/** Who money would go to, shown before the customer confirms. An account in another bank has no local account. */
public record Recipient(String name, String bank, Account account) {

    public boolean isOwnBank() {
        return account != null;
    }
}
