package com.example.bank.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A fixed-term deposit. The amount is in the deposit's own currency. It knows
 * its own arithmetic: how many months it runs and what each payout is.
 */
public record TermDeposit(int number, String username, String ownerName, String type, Money amount, String duration,
                          String interest, String payment, String currency, String date) {

    /** "3 months" is 3, "2 years" is 24. (The old screen read both as a number of years.) */
    public static int months(String period) {
        if (period == null || period.isBlank()) return 0;
        int n = Integer.parseInt(period.replaceAll("\\D", ""));
        return period.toLowerCase().contains("year") ? n * 12 : n;
    }

    public int months() {
        return months(duration);
    }

    public BigDecimal interestRate() {
        return interest == null || interest.isBlank() ? BigDecimal.ZERO
                : new BigDecimal(interest.replace("%", "").strip()).divide(BigDecimal.valueOf(100));
    }

    /** What is paid out every payment period: the amount with its interest, spread over the term. */
    public Money periodicPayout() {
        int total = months();
        int every = months(payment);
        if (total == 0) return amount;
        if (every == 0) every = total; // interest-free: all at the end
        BigDecimal value = amount.amount().multiply(BigDecimal.ONE.add(interestRate()));
        return new Money(value.multiply(BigDecimal.valueOf(every)).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_EVEN));
    }

    public DepositCurrency currencyUnit() {
        return DepositCurrency.of(currency);
    }

    /** What the deposit is worth in rials, which is what is returned when it is closed. */
    public Money valueInRials() {
        return currencyUnit().toRials(amount);
    }
}
