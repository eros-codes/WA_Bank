package com.example.bank.model;

import java.util.Optional;

/** Iranian mobile operators, recognised from the start of the number (09xx). */
public enum MobileOperator {
    RIGHTEL("Rightel"),
    HAMRAH_AVAL("Hamrah aval"),
    IRANCELL("Irancell");

    private final String label;

    MobileOperator(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** From the first four digits, e.g. "0912". */
    public static Optional<MobileOperator> of(String phone) {
        if (phone == null || phone.length() < 4 || !phone.startsWith("09")) return Optional.empty();
        int code = Integer.parseInt(phone.substring(1, 4));
        if (code >= 920 && code <= 929) return Optional.of(RIGHTEL);
        if (code >= 910 && code <= 919 || code >= 990 && code <= 997) return Optional.of(HAMRAH_AVAL);
        if (code >= 901 && code <= 909 || code >= 930 && code <= 939) return Optional.of(IRANCELL);
        return Optional.empty();
    }
}
