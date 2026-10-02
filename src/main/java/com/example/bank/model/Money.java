package com.example.bank.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * An amount of money. Never a double: doubles cannot hold most decimal
 * amounts exactly, and a bank cannot round by accident. Kept to two decimal
 * places; shown without them when the amount is whole.
 */
public record Money(BigDecimal amount) implements Comparable<Money> {

    public static final Money ZERO = new Money(BigDecimal.ZERO);

    public Money {
        if (amount == null) throw new IllegalArgumentException("amount is null");
        amount = amount.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static Money of(long whole) {
        return new Money(BigDecimal.valueOf(whole));
    }

    /**
     * Reads an amount as typed by a person or as stored by the old code:
     * "1,500,000", "۱۵۰۰۰۰۰", "1500000.0" and "1.5E7" all work.
     */
    public static Money parse(String text) {
        if (text == null) throw new IllegalArgumentException("No amount given");
        StringBuilder s = new StringBuilder();
        for (char ch : text.strip().toCharArray()) {
            if (ch >= '۰' && ch <= '۹') s.append((char) ('0' + (ch - '۰')));       // Persian digits
            else if (ch >= '٠' && ch <= '٩') s.append((char) ('0' + (ch - '٠')));  // Arabic digits
            else if (ch == ',' || ch == '٬' || ch == ' ' || ch == '_') continue;
            else s.append(ch);
        }
        try {
            return new Money(new BigDecimal(s.toString()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("\"" + text + "\" is not an amount of money");
        }
    }

    public Money plus(Money other) {
        return new Money(amount.add(other.amount));
    }

    public Money minus(Money other) {
        return new Money(amount.subtract(other.amount));
    }

    public Money times(BigDecimal factor) {
        return new Money(amount.multiply(factor));
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public boolean isAtLeast(Money other) {
        return compareTo(other) >= 0;
    }

    @Override
    public int compareTo(Money other) {
        return amount.compareTo(other.amount);
    }

    /** How it is written to the database: plain digits, never "1.5E7". */
    public String toDbString() {
        return amount.stripTrailingZeros().scale() <= 0 ? amount.setScale(0, RoundingMode.UNNECESSARY).toPlainString() : amount.toPlainString();
    }

    /** How it is shown: "1,500,000" or "1,500,000.50". */
    public String format() {
        DecimalFormat f = new DecimalFormat(amount.stripTrailingZeros().scale() <= 0 ? "#,##0" : "#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
        return f.format(amount);
    }

    @Override
    public String toString() {
        return format();
    }
}
