package com.example.bank.model;

/**
 * The bills the app can pay, each with one amount used both for showing and
 * for charging. The telephone bill used to show 400,000 IRR and charge 4,000,000.
 */
public enum BillType {
    WATER("Water Bill", 3_000_000),
    ELECTRIC("Electric Bill", 4_000_000),
    GAS("Gas Bill", 2_000_000),
    TELEPHONE("Telephone Bill", 400_000),
    INTERNET("Internet Bill", 3_000_000);

    private final String label;
    private final Money amount;

    BillType(String label, long amount) {
        this.label = label;
        this.amount = Money.of(amount);
    }

    public String label() {
        return label;
    }

    public Money amount() {
        return amount;
    }
}
