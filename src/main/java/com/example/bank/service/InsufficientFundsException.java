package com.example.bank.service;

public class InsufficientFundsException extends BankException {

    private static final long serialVersionUID = 1L;
    public InsufficientFundsException() {
        super("There is not enough money in the account.");
    }
}
