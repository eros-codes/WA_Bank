package com.example.bank.model;

/** A cheque's review state. */
public enum ChequeStatus implements DbEnum {
    PENDING("pending", "Pending"),
    ACCEPTED("accepted", "accepted"),
    DENIED("denied", "denied");

    private final String dbValue;
    private final String label;

    ChequeStatus(String dbValue, String label) {
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

    public static ChequeStatus fromDb(String value) {
        return DbEnum.parse(ChequeStatus.class, value);
    }
}
