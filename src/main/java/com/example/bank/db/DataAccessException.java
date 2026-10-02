package com.example.bank.db;

/** A database operation failed. Unchecked, so it reaches the screen that can tell the user. */
public class DataAccessException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
