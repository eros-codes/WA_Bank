package com.example.bank.repository.jdbc;

import com.example.bank.db.Database;
import com.example.bank.model.Money;
import com.example.bank.repository.BankFundsRepository;

import java.sql.Connection;
import java.sql.SQLException;

public final class JdbcBankFundsRepository implements BankFundsRepository {

    private final Database db;

    public JdbcBankFundsRepository(Database db) {
        this.db = db;
    }

    @Override
    public Money balance() {
        return db.queryOne("SELECT balance FROM bank_funds WHERE id = 1", r -> new Money(r.getBigDecimal(1))).orElse(Money.ZERO);
    }

    @Override
    public void add(Connection c, Money amount) throws SQLException {
        Database.update(c, "UPDATE bank_funds SET balance = balance + ? WHERE id = 1", amount.amount());
    }

    @Override
    public boolean take(Connection c, Money amount) throws SQLException {
        return Database.update(c, "UPDATE bank_funds SET balance = balance - ? WHERE id = 1 AND balance >= ?", amount.amount(), amount.amount()) == 1;
    }
}
