package com.example.bank.model;

/** An account's state: waiting for an employee, in use, or closed (closed accounts keep their history). */
public enum AccountStatus implements DbEnum {
    PENDING("pending", "Pending"),
    ACCEPTED("active", "Accepted"),
    DENIED("denied", "Denied"),
    CLOSED("closed", "Closed");

    private final String dbValue;
    private final String label;

    AccountStatus(String dbValue, String label) {
        this.dbValue = dbValue;
        this.label = label;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }

    /** How it is shown on screen. */
    public String label() {
        return label;
    }

    public static AccountStatus fromDb(String value) {
        return DbEnum.parse(AccountStatus.class, value);
    }
}
