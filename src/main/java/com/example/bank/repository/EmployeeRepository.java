package com.example.bank.repository;

import com.example.bank.model.Employee;

import java.util.List;
import java.util.Optional;

/** The people who have one staff role (EMPLOYEE or MANAGER). */
public interface EmployeeRepository {

    Optional<Employee> find(String username);

    String passwordHashOf(String username);

    void updatePasswordHash(String username, String hash);

    List<Employee> findAll();

    void create(Employee employee, String passwordHash);

    void delete(String username);
}
