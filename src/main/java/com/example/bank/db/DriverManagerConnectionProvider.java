package com.example.bank.db;

import com.example.bank.config.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Opens connections with the settings from {@link AppConfig}: the only place that knows the credentials. */
public final class DriverManagerConnectionProvider implements ConnectionProvider {

    private final AppConfig config;

    public DriverManagerConnectionProvider(AppConfig config) {
        this.config = config;
    }

    @Override
    public Connection open() throws SQLException {
        return DriverManager.getConnection(config.dbUrl(), config.dbUser(), config.dbPassword());
    }
}
