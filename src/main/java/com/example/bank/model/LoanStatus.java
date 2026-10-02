package com.example.bank.model;

/** Where a loan request is. An employee reviews it first (PENDING); the manager makes the final decision (FINAL_STEP). */
public enum LoanStatus implements DbEnum {
    PENDING("pending", "pending"),
    FINAL_STEP("final_step", "Final step"),
    ACCEPTED("accepted", "accepted"),
    DENIED("denied", "denied");

    private final String dbValue;
    private final String label;

    LoanStatus(String dbValue, String label) {
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

    public static LoanStatus fromDb(String value) {
        return DbEnum.parse(LoanStatus.class, value);
    }
}
