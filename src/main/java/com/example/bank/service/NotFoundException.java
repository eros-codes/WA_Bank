package com.example.bank.service;

public class NotFoundException extends BankException {

    private static final long serialVersionUID = 1L;
    public NotFoundException(String message) {
        super(message);
    }
}
