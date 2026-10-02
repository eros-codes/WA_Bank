package com.example.bank.model;

/** What kind of account it is. */
public enum AccountType implements DbEnum {
    CHECKING("checking"),
    SAVING("saving");

    private final String dbValue;

    AccountType(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }

    public static AccountType fromDb(String value) {
        return DbEnum.parse(AccountType.class, value);
    }
}
