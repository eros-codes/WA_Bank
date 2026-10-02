# Tests

These run against a real MySQL or MariaDB database, so they are not part of `mvn test`.
Use a throwaway database: the tests change balances and passwords.

## Two kinds of test database

**Upgraded**, like a database from the old app (CoreTest and the screen tests):

```bash
mysql -u root -p -e "CREATE DATABASE bank_test"
mysql -u root -p bank_test < tests/legacy_schema.sql   # the old app's tables
mysql -u root -p bank_test < tests/legacy_rows.sql     # rows as it stored them: "1.5E7", plain passwords
mysql -u root -p bank_test < database/migrations/001_fix_money_keys_and_passwords.sql
mysql -u root -p bank_test < database/migrations/002_bank_funds.sql
mysql -u root -p bank_test < tests/staff_rows.sql      # for the screen tests
mysql -u root -p bank_test < tests/upgrade_rows.sql    # optional: hard cases for 003 (a promoted customer, a shared phone, an old loan term ...)
mysql -u root -p bank_test < database/migrations/003_normalize.sql
```

**New**, from `database/schema.sql` alone (FreshInstallTest).

## FreshInstallTest: a new installation

The first manager added with the README's SQL, two customers who sign up and are approved, a
transfer to a customer and one to another bank, a bill (and the refusal to pay it twice), a
deposit opened and closed, a cheque, a loan through an employee and the manager, a customer
promoted to employee and removed again. It connects to a database named `fresh`.

## CoreTest: the banking rules, no screens

Checks password upgrades, transfers and their fees, the negative-amount attack, transfers to other
banks, the IBAN methods, rollback on insufficient funds, twenty simultaneous withdrawals, loans
decided by id, the history and the profile update.

It connects to `jdbc:mysql://127.0.0.1:3306/mig` as `root` with no password; change the first line
of `main` to point at your test database.

## StaffSmokeTest and CustomerSmokeTest: the screens

They open every screen of the employee, manager and customer apps and drive the main flows by
filling fields and pressing buttons (a transfer, a bill, a deposit, a cheque, a password change,
approving a loan). Load `tests/staff_rows.sql` too, then set `BANK_ENV_FILE` to a `.env` that points
at the test database. They are JavaFX applications in the `com.example.staff` and `com.example.customer` packages: put
them under `src/main/java` temporarily, or compile them with the project's classes on the
classpath, and run their `main`.
