package com.example.bank.model;

import java.math.BigDecimal;

/**
 * The currencies a deposit can be held in, with the rate used to take rials
 * from the customer's account. These are the demo rates the app was built
 * with (1 USD = 100 IRR, 1 EUR = 110 IRR), not market rates.
 */
public enum DepositCurrency {
    IRR(1), USD(100), EUR(110);

    private final BigDecimal rialsPerUnit;

    DepositCurrency(long rialsPerUnit) {
        this.rialsPerUnit = BigDecimal.valueOf(rialsPerUnit);
    }

    public Money toRials(Money amountInThisCurrency) {
        return amountInThisCurrency.times(rialsPerUnit);
    }

    public static DepositCurrency of(String code) {
        return code == null ? IRR : valueOf(code.strip().toUpperCase());
    }
}
