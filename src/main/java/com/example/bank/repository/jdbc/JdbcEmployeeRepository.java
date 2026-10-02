package com.example.bank.repository.jdbc;

import com.example.bank.db.Database;
import com.example.bank.model.Employee;
import com.example.bank.repository.EmployeeRepository;

import java.util.List;
import java.util.Optional;

/**
 * The people with one staff role. Staff are users like customers; a customer
 * who becomes an employee is the same user with one more role.
 */
public final class JdbcEmployeeRepository implements EmployeeRepository {

    private final Database db;
    private final String role;
    private final String members;

    /** @param role "EMPLOYEE" or "MANAGER" */
    public JdbcEmployeeRepository(Database db, String role) {
        if (!role.equals("EMPLOYEE") && !role.equals("MANAGER")) throw new IllegalArgumentException(role);
        this.db = db;
        this.role = role;
        this.members = "FROM users u JOIN user_roles ur ON ur.user_id = u.user_id JOIN roles r ON r.role_id = ur.role_id "
                + "WHERE r.role_name = '" + role + "' AND u.status = 'active'";
    }

    @Override
    public Optional<Employee> find(String username) {
        return db.queryOne("SELECT u.username, u.full_name " + members + " AND u.username = ?", r -> new Employee(r.getString(1), r.getString(2)), username);
    }

    @Override
    public String passwordHashOf(String username) {
        return db.queryOne("SELECT u.password_hash " + members + " AND u.username = ?", r -> r.getString(1), username).orElse(null);
    }

    @Override
    public void updatePasswordHash(String username, String hash) {
        db.update("UPDATE users SET password_hash = ? WHERE username = ?", hash, username);
    }

    @Override
    public List<Employee> findAll() {
        return db.query("SELECT u.username, u.full_name " + members + " ORDER BY u.full_name", r -> new Employee(r.getString(1), r.getString(2)));
    }

    /** Gives the role to an existing user, or creates the user (staff need no national ID). */
    @Override
    public void create(Employee e, String passwordHash) {
        db.inTransaction(c -> {
            if (Database.queryOne(c, "SELECT 1 FROM users WHERE username = ?", r -> true, e.username()).isEmpty()) {
                Database.update(c, "INSERT INTO users (username, password_hash, full_name) VALUES (?, ?, ?)", e.username(), passwordHash, e.fullName());
            }
            Database.update(c, "INSERT IGNORE INTO user_roles (user_id, role_id) SELECT u.user_id, r.role_id FROM users u, roles r "
                    + "WHERE u.username = ? AND r.role_name = ?", e.username(), role);
            return null;
        });
    }

    /** Takes the role away. The person stays: they may also be a customer. */
    @Override
    public void delete(String username) {
        db.update("DELETE ur FROM user_roles ur JOIN users u ON u.user_id = ur.user_id JOIN roles r ON r.role_id = ur.role_id "
                + "WHERE u.username = ? AND r.role_name = ?", username, role);
    }
}
