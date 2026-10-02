package com.example.bank.ui;

import java.util.Optional;

/**
 * Who is signed in to this copy of the app. Replaces the static
 * "currentUsername" fields that were spread over several controllers.
 */
public final class Session {

    private static String username;
    private static String fullName;

    private Session() {
    }

    public static void signIn(String user, String name) {
        username = user;
        fullName = name;
    }

    public static void signOut() {
        username = null;
        fullName = null;
    }

    /** The signed-in user, or an error if a screen is opened without one. */
    public static String username() {
        if (username == null) throw new IllegalStateException("You are not signed in.");
        return username;
    }

    public static Optional<String> fullName() {
        return Optional.ofNullable(fullName);
    }

    public static boolean signedIn() {
        return username != null;
    }
}
