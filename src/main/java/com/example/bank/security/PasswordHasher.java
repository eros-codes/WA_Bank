package com.example.bank.security;

/** Turns passwords into something that can be stored, and checks a password against it. */
public interface PasswordHasher {

    String hash(String password);

    boolean matches(String password, String stored);

    /** True when the stored value should be replaced by a fresh hash (it is old or weaker). */
    boolean needsRehash(String stored);
}
