package com.example.bank.model;

import java.math.BigDecimal;

/**
 * The three ways to send money to an IBAN, each with its own limits and fee.
 * The old code compared against "satena" while the button set "satna", so
 * Satna's limits were never applied.
 */
public enum IbanMethod {
    PAYA("Paya", null, Money.of(1_000_000_000), new BigDecimal("0.01"), Money.ZERO),
    SATNA("Satna", Money.of(500_000_000), Money.of(2_000_000_000), new BigDecimal("0.02"), Money.ZERO),
    INSTANT("Instant", null, Money.of(150_000_000), BigDecimal.ZERO, Money.of(500));

    private final String label;
    private final Money minimum;
    private final Money maximum;
    private final BigDecimal rate;
    private final Money flatFee;

    IbanMethod(String label, Money minimum, Money maximum, BigDecimal rate, Money flatFee) {
        this.label = label;
        this.minimum = minimum;
        this.maximum = maximum;
        this.rate = rate;
        this.flatFee = flatFee;
    }

    public String label() {
        return label;
    }

    /** Why this amount cannot be sent this way, or null if it can. */
    public String refusal(Money amount) {
        if (minimum != null && amount.compareTo(minimum) < 0) {
            return label + " is for " + minimum.format() + " IRR or more. Use Paya or Instant for smaller amounts.";
        }
        if (amount.compareTo(maximum) > 0) {
            return label + " allows at most " + maximum.format() + " IRR.";
        }
        return null;
    }

    public Money fee(Money amount) {
        return amount.times(rate).plus(flatFee);
    }
}
