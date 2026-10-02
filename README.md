# WA Bank

A desktop banking system in Java, JavaFX and MySQL, made of three apps that share one database:

- **Customer app**: sign up, transfers (card to card, account to account by drag and drop, IBAN by
  Paya, Satna or Instant), bills and mobile top-ups, cheques, fixed-term deposits (rial,
  foreign-currency and interest-free), loan applications, history with PDF receipts.
- **Employee app**: verify new accounts, first review of loans, review cheques, see transfers,
  payments and deposits.
- **Manager app**: the final decision on loans, customers and staff, salaries.

## Team

WA Bank began as a university project by three people:

- **Arvin Saghafi**: all of the Java code: the banking logic, the data model and the database layer.
- **Amirali Doustiyar**: the customer app's screens (JavaFX and FXML).
- **Fatemeh Pourali**: the employee and manager apps' screens (JavaFX and FXML).

## Running it

You need JDK 23, Maven and MySQL 8.

### 1. The database

A new installation:

```bash
mysql -u root -p -e "CREATE DATABASE userdetail"
mysql -u root -p userdetail < database/schema.sql
mysql -u root -p < database/create_app_user.sql        # a user for the app that is not root
```

Already have a `userdetail` database from an earlier version? Back it up
(`mysqldump -u root -p userdetail > backup.sql`) and run the migrations you have not run yet, in
order, as root: `001`, `002`, then `003`. They keep every row: amounts stored as text become
numbers, each customer becomes one user with their accounts under them, staff become users with a
role, and old passwords keep working (they are hashed at the next sign-in). `003` keeps the old
tables, renamed `legacy_*`, and ends with a report comparing the counts and the total balance
before and after.

### 2. The settings

```bash
cp .env.example .env
```

Put the database user and password from step 1 in `.env`. It is in `.gitignore`; the password is
never written in the code.

### 3. Start an app

```bash
mvn clean javafx:run              # customer
mvn clean javafx:run@employee     # employee
mvn clean javafx:run@manager      # manager
```

The first manager is added by hand. The password may be typed as plain text here: it is
accepted once and replaced by a hash at their first sign-in.

```sql
INSERT INTO users (username, password_hash, full_name) VALUES ('manager', 'ChooseAStrong1', 'Your Name');
INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id FROM users u, roles r WHERE u.username = 'manager' AND r.role_name = 'MANAGER';
```

Employees are made by the manager, from a customer's page ("promote"): the same person, with one more role.

## How it is built

```
com.example.customer   the customer app's screens           ┐
com.example.staff      the employee and manager screens     ┘ JavaFX, no SQL
        │
com.example.bank.service      the banking rules: transfers, payments, loans, deposits, sign-up
        │
com.example.bank.repository   interfaces for reading and writing data
   └── jdbc                   their MySQL implementations
        │
com.example.bank.db           one gateway to the database, with transactions
com.example.bank.config       settings from .env
com.example.bank.security     password hashing
com.example.bank.model        accounts, money, loans, deposits ... as records and enums
com.example.bank.app          AppContext: where everything is created and wired together
```

Some of the design:

- **Money is never a `double`.** `Money` wraps `BigDecimal`, so amounts are exact.
- **Every movement of money is one database transaction.** A withdrawal is a single conditional
  update (`deposit = deposit - ? WHERE ... AND deposit >= ?`), so a balance can never go below
  zero, even with two operations at the same moment. The database also refuses negative balances.
- **Interfaces where things can vary:** `ConnectionProvider`, `PasswordHasher`, the repositories,
  `StaffRole` (what differs between an employee and the manager), `Card<T>` (the tiles in lists),
  `Sectioned` (screens with several sections), `Dialogs.Presenter` (how messages are shown).
- **One base class per app** (`CustomerPage`, `StaffPage`): each screen keeps only its own work.
  The employee and manager apps use the same screen classes; the role decides the rest.
- **Screens change inside one window** instead of opening a new window for every click.

The database is normalised: a person (`users`) has roles (`user_roles`: customer, employee,
manager) and accounts (`accounts`); transfers, deposits, bills, top-ups and cheques point at
accounts with foreign keys. Nothing that has history is deleted: closing an account, a customer or
a deposit marks it closed. Fees and taxes are recorded with each payment.

## The bank's rules

| | |
|---|---|
| Card to card | 7,200 IRR fee, to any Iranian bank card (the bank is recognised from the number) |
| Account to account | free, within the bank |
| IBAN, Paya | up to 1,000,000,000 IRR, 1% fee to other banks |
| IBAN, Satna | 500,000,000 to 2,000,000,000 IRR, 2% fee to other banks |
| IBAN, Instant | up to 150,000,000 IRR, 500 IRR fee to other banks |
| Bills and top-ups | 9% tax |
| Opening deposit | at least 50,000 IRR |
| Loans | up to 5,000,000,000 IRR, 1 to 5 years, 12% to 22% |

Fees and taxes go to the bank's own funds (the `bankfunds` table) in the same transaction as the payment.

## Security

- Passwords are hashed with PBKDF2-HMAC-SHA256 and a random salt. A new customer gets a random
  first password, shown once; their username is random too, not derived from their IBAN.
- The national ID is checked with its real checksum.
- Customers and staff can only act on their own accounts; a username cannot be swapped mid-way.

Two things are simulated, because this project has no SMS gateway or exchange-rate feed: the
one-time codes for payments are shown in a message instead of sent by SMS, and the foreign-currency
deposits use fixed demo rates (1 USD = 100 IRR, 1 EUR = 110 IRR). Forgotten passwords are reset at
a branch.

## Tests

`tests/` has integration tests that run against a real database: the banking rules on an upgraded
database, a new installation from `schema.sql`, and two that drive every screen of the three apps. See [tests/README.md](tests/README.md).
