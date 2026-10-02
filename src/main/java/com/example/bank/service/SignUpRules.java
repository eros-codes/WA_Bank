package com.example.bank.service;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Period;

/**
 * The checks a sign-up must pass. Each throws a BankException with a message
 * for the user. The old checks had holes: the national-ID checksum loop never
 * ran, "month == 4 && month == 6" let April 31 through, the year check let 1800
 * through, and the age limit was the fixed year 2006.
 */
public final class SignUpRules {

    public static final int MINIMUM_AGE = 18;

    private SignUpRules() {
    }

    public static void fullName(String name) {
        if (name == null || name.isBlank()) throw new BankException("Enter your full name.");
        if (name.strip().split("\\s+").length < 2) throw new BankException("Enter your full name: first name and last name.");
    }

    /** A real date, and an adult today. */
    public static LocalDate birthDate(String year, String month, String day, LocalDate today) {
        LocalDate born;
        try {
            born = LocalDate.of(Integer.parseInt(year.strip()), Integer.parseInt(month.strip()), Integer.parseInt(day.strip()));
        } catch (NumberFormatException | DateTimeException e) {
            throw new BankException("Enter a real date of birth.");
        }
        if (born.getYear() < 1900 || born.isAfter(today)) throw new BankException("Enter a real date of birth.");
        if (Period.between(born, today).getYears() < MINIMUM_AGE) throw new BankException("Come back when you are " + MINIMUM_AGE + " :)");
        return born;
    }

    public static void mobile(String phone) {
        if (phone == null || !phone.matches("09\\d{9}")) throw new BankException("Enter a mobile number like 09123456789.");
    }

    public static void email(String email) {
        if (email == null || !email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) throw new BankException("Enter a valid email address.");
    }

    /** The Iranian national ID (code melli): ten digits, the last one a checksum of the others. */
    public static void nationalId(String id) {
        if (id == null || !id.matches("\\d{10}") || id.chars().distinct().count() == 1) throw new BankException("Enter a valid national ID.");
        int sum = 0;
        for (int i = 0; i < 9; i++) sum += (id.charAt(i) - '0') * (10 - i);
        int r = sum % 11;
        int check = id.charAt(9) - '0';
        if (!(r < 2 ? check == r : check == 11 - r)) throw new BankException("This national ID is not valid.");
    }

    public static void wholeNumber(String value, String what) {
        if (value == null || !value.strip().matches("\\d+")) throw new BankException("Enter " + what + " as a number.");
    }

    public static void required(String value, String what) {
        if (value == null || value.isBlank()) throw new BankException(what + " is required.");
    }
}
