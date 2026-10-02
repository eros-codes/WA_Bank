package com.example.bank.service;

/** A request the bank refuses, with a message that can be shown to the user as it is. */
public class BankException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    public BankException(String message) {
        super(message);
    }
}
