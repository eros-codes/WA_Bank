package com.example.bank.model;

/**
 * A card's expiry, written "year.month". Typed "2030.5" or stored "2030.05",
 * it is the same month, so expiries are compared as numbers, not as text.
 */
public final class CardExpiry {

    private CardExpiry() {
    }

    public static boolean same(String stored, String typed) {
        int[] a = parse(stored), b = parse(typed);
        return a != null && b != null && a[0] == b[0] && a[1] == b[1];
    }

    private static int[] parse(String s) {
        if (s == null) return null;
        String[] parts = s.strip().split("[./-]");
        if (parts.length != 2) return null;
        try {
            return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
