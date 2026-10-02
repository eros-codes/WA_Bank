package com.example.bank.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * PBKDF2 with HMAC-SHA256, from the JDK itself. Stored as
 * {@code pbkdf2$<iterations>$<salt>$<hash>}. Passwords saved before hashing
 * existed (plain text) are still accepted once, and {@link #needsRehash}
 * tells the caller to replace them with a hash at that moment.
 */
public final class Pbkdf2PasswordHasher implements PasswordHasher {

    private static final String PREFIX = "pbkdf2$";
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;

    private final int iterations;
    private final SecureRandom random = new SecureRandom();

    public Pbkdf2PasswordHasher(int iterations) {
        if (iterations < 10_000) throw new IllegalArgumentException("Too few PBKDF2 iterations: " + iterations);
        this.iterations = iterations;
    }

    @Override
    public String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        Base64.Encoder b64 = Base64.getEncoder().withoutPadding();
        return PREFIX + iterations + "$" + b64.encodeToString(salt) + "$" + b64.encodeToString(derive(password, salt, iterations));
    }

    @Override
    public boolean matches(String password, String stored) {
        if (password == null || stored == null) return false;
        if (!stored.startsWith(PREFIX)) {
            // a password from before hashing: compare in constant time, then the caller rehashes it
            return MessageDigest.isEqual(password.getBytes(), stored.getBytes());
        }
        String[] parts = stored.split("\\$");
        if (parts.length != 4) return false;
        try {
            int rounds = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            return MessageDigest.isEqual(derive(password, salt, rounds), expected);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public boolean needsRehash(String stored) {
        if (stored == null || !stored.startsWith(PREFIX)) return true;
        String[] parts = stored.split("\\$");
        return parts.length != 4 || Integer.parseInt(parts[1]) < iterations;
    }

    private static byte[] derive(String password, byte[] salt, int rounds) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, rounds, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 is not available", e);
        } finally {
            spec.clearPassword();
        }
    }
}
