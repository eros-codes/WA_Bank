package com.example.bank.config;

/** Everything the app reads from its environment, typed and in one place. */
public record AppConfig(String dbUrl, String dbUser, String dbPassword, int passwordHashIterations) {

    public static AppConfig fromEnv(Env env) {
        return new AppConfig(
                env.require("DB_URL"),
                env.require("DB_USER"),
                env.require("DB_PASSWORD"),
                env.get("PASSWORD_HASH_ITERATIONS").map(Integer::parseInt).orElse(210_000));
    }

    /** The password stays out of logs and error messages. */
    @Override
    public String toString() {
        return "AppConfig[dbUrl=" + dbUrl + ", dbUser=" + dbUser + ", dbPassword=***]";
    }
}
