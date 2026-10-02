-- =====================================================================
-- 003: from the old tables to the normalised design.
--
-- Run it once, on a database that has had 001 and 002. Back up first:
--   mysqldump -u root -p userdetail > backup.sql
--
-- What it does:
--   * every old customer row becomes a user, with one account per row
--     (a customer with two accounts was two copies of the same person);
--   * employees and managers become users with the EMPLOYEE or MANAGER role;
--     a promoted customer is one user with both roles;
--   * transfers, deposits, loans (with their documents), bills, top-ups and
--     cheques are copied, keeping their old numbers, so receipts stay valid;
--   * the old tables are kept, renamed legacy_*, in case anything is needed.
--
-- If it stops with an error, nothing has been renamed yet: restore the
-- backup, fix the cause, and run it again. Run a second time on purpose, it
-- stops at the first CREATE TABLE, because the new tables already exist.
-- =====================================================================

-- MySQL Workbench refuses UPDATEs that touch every row (error 1175).
SET SQL_SAFE_UPDATES = 0;
-- Lenient for the copy: a date that cannot be read becomes NULL instead of
-- stopping the script. The report at the end lists what could not be read.
SET @saved_sql_mode = @@SESSION.sql_mode;
SET SESSION sql_mode = 'NO_ENGINE_SUBSTITUTION';

-- The new tables are created with the same character set and collation as
-- the old ones (utf8mb4_0900_ai_ci, MySQL 8's default), so text in old and
-- new tables can be compared while copying.

-- ================= the new tables =================

-- People: customers and staff. One row per person, however many accounts they have.
CREATE TABLE users (
    user_id INT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    national_id VARCHAR(50) NULL,              -- staff added before this version have none; the app requires it for customers
    birth_date DATE,
    gender VARCHAR(20),
    marital_status VARCHAR(20),
    nationality VARCHAR(50),
    address VARCHAR(255),
    postal_code VARCHAR(20),
    phone_number VARCHAR(20),
    email VARCHAR(100),
    occupation VARCHAR(100),
    employer_address VARCHAR(255),
    annual_income DECIMAL(15,2),
    id_photo MEDIUMBLOB,                       -- the national ID card photo an employee checks before approving
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'active',

    PRIMARY KEY (user_id),

    UNIQUE KEY uq_users_username (username),
    UNIQUE KEY uq_users_national_id (national_id),
    UNIQUE KEY uq_users_phone_number (phone_number),
    UNIQUE KEY uq_users_email (email)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE roles (
    role_id INT NOT NULL AUTO_INCREMENT,
    role_name VARCHAR(50) NOT NULL,

    PRIMARY KEY (role_id),

    UNIQUE KEY uq_roles_role_name (role_name)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO roles (role_name) VALUES ('CUSTOMER'), ('EMPLOYEE'), ('MANAGER');


CREATE TABLE user_roles (
    user_id INT NOT NULL,
    role_id INT NOT NULL,
    assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id, role_id),

    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id)
        REFERENCES roles(role_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- statement_status: 'pending' (waiting for an employee), 'active', or 'closed'.
-- Accounts are closed, not deleted, so their history stays.
CREATE TABLE accounts (
    account_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    card_number VARCHAR(50) NOT NULL,
    iban VARCHAR(50) NOT NULL,
    cvv VARCHAR(10) NOT NULL,
    expire_date DATE NOT NULL,                 -- the last day of the card's expiry month
    account_type VARCHAR(50) NOT NULL,
    balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    statement_status VARCHAR(20) NOT NULL DEFAULT 'pending',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (account_id),

    UNIQUE KEY uq_accounts_account_number (account_number),
    UNIQUE KEY uq_accounts_card_number (card_number),
    UNIQUE KEY uq_accounts_iban (iban),
    KEY ix_accounts_user (user_id),

    CONSTRAINT chk_accounts_balance_not_negative CHECK (balance >= 0),

    CONSTRAINT fk_accounts_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- A transfer. The destination account is NULL when the money went to another
-- bank; who and where it went is then in the destination_* columns. The source
-- is NULL only for rows imported from the old app whose account was not found.
CREATE TABLE transactions (
    transaction_id INT NOT NULL AUTO_INCREMENT,
    source_account_id INT NULL,
    destination_account_id INT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    fee DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    destination_name VARCHAR(255),
    destination_card VARCHAR(50),
    destination_iban VARCHAR(50),
    destination_bank_name VARCHAR(100),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'completed',

    PRIMARY KEY (transaction_id),
    KEY ix_transactions_created (created_at),

    CONSTRAINT fk_transactions_source_account
        FOREIGN KEY (source_account_id)
        REFERENCES accounts(account_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_transactions_destination_account
        FOREIGN KEY (destination_account_id)
        REFERENCES accounts(account_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- status: 'active' or 'closed'. A closed deposit's money went back to its owner.
CREATE TABLE deposits (
    deposit_id INT NOT NULL AUTO_INCREMENT,
    account_id INT NOT NULL,
    type VARCHAR(50) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,             -- in the deposit's own currency
    duration_months INT,
    interest_rate DECIMAL(5,2),
    payout_type VARCHAR(50),
    currency VARCHAR(10) NOT NULL,
    opened_at DATE NOT NULL,
    maturity_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'active',

    PRIMARY KEY (deposit_id),

    CONSTRAINT fk_deposits_account
        FOREIGN KEY (account_id)
        REFERENCES accounts(account_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- status: 'pending' (employee review), 'final_step' (manager), 'accepted', 'denied'
CREATE TABLE loans (
    loan_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    loan_amount DECIMAL(15,2) NOT NULL,
    term_months INT NOT NULL,
    interest_rate DECIMAL(5,2) NOT NULL,
    request_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',

    PRIMARY KEY (loan_id),
    KEY ix_loans_status (status),

    CONSTRAINT fk_loans_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- The documents themselves are kept in the database, so they are backed up
-- with everything else and saved in the same transaction as the loan.
CREATE TABLE loan_documents (
    document_id INT NOT NULL AUTO_INCREMENT,
    loan_id INT NOT NULL,
    document_type VARCHAR(50) NOT NULL,        -- 'id', 'employment', 'collateral', 'deed'
    file_name VARCHAR(255),
    content MEDIUMBLOB NOT NULL,
    uploaded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (document_id),

    CONSTRAINT fk_loan_documents_loan
        FOREIGN KEY (loan_id)
        REFERENCES loans(loan_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- bill_identifier is unique: a bill can only be paid once.
CREATE TABLE bill_payments (
    payment_id INT NOT NULL AUTO_INCREMENT,
    account_id INT NOT NULL,
    bill_type VARCHAR(50) NOT NULL,
    bill_identifier VARCHAR(100) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    tax DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    paid_by VARCHAR(100),
    paid_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    due_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'paid',

    PRIMARY KEY (payment_id),

    UNIQUE KEY uq_bill_payments_bill (bill_identifier),

    CONSTRAINT fk_bill_payments_account
        FOREIGN KEY (account_id)
        REFERENCES accounts(account_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE mobile_charges (
    charge_id INT NOT NULL AUTO_INCREMENT,
    account_id INT NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    tax DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    charged_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (charge_id),

    CONSTRAINT fk_mobile_charges_account
        FOREIGN KEY (account_id)
        REFERENCES accounts(account_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- status: 'pending', 'accepted', 'denied'
CREATE TABLE checks (
    check_id INT NOT NULL AUTO_INCREMENT,
    issuer_account_id INT NOT NULL,
    recipient_name VARCHAR(100) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',

    PRIMARY KEY (check_id),

    CONSTRAINT fk_checks_issuer_account
        FOREIGN KEY (issuer_account_id)
        REFERENCES accounts(account_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- The bank's own money: fees and taxes come in, salaries go out. One row.
CREATE TABLE bank_funds (
    id TINYINT NOT NULL,
    balance DECIMAL(15,2) NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT chk_bank_funds_single_row CHECK (id = 1),
    CONSTRAINT chk_bank_funds_not_negative CHECK (balance >= 0)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- ================= people =================

-- One user per old username: the first row of each. If two different people
-- shared a national ID, phone number or email, the first keeps it and the
-- others are left empty (they are listed in the report).
INSERT INTO users (user_id, username, password_hash, full_name, national_id, birth_date, gender, marital_status, nationality,
                   address, postal_code, phone_number, email, occupation, employer_address, annual_income, id_photo, created_at, status)
SELECT p.account_id, CAST(p.username AS CHAR), p.password, p.fullname,
       IF(p.nid_rank = 1, NULLIF(TRIM(p.id), ''), NULL),
       CASE WHEN p.birth REGEXP '^[0-9]{4}[./-][0-9]{1,2}[./-][0-9]{1,2}$'
            THEN STR_TO_DATE(REPLACE(REPLACE(p.birth, '/', '.'), '-', '.'), '%Y.%c.%e') END,
       p.gender, p.marital, p.national, p.address, p.postalcode,
       IF(p.phone_rank = 1, NULLIF(TRIM(p.phonenumber), ''), NULL),
       IF(p.email_rank = 1, NULLIF(TRIM(p.email), ''), NULL),
       p.occupation, p.employeraddress,
       IF(TRIM(p.annualincome) REGEXP '^[0-9]+(\\.[0-9]+)?$', TRIM(p.annualincome), NULL),
       IF(LENGTH(p.idphoto) > 0, p.idphoto, NULL),
       COALESCE(STR_TO_DATE(p.creationdate, '%Y-%m-%d %H:%i'), STR_TO_DATE(p.creationdate, '%Y-%m-%d'), NOW()),
       'active'
FROM (
    SELECT f.*,
           ROW_NUMBER() OVER (PARTITION BY NULLIF(TRIM(f.id), '') ORDER BY f.account_id) AS nid_rank,
           ROW_NUMBER() OVER (PARTITION BY NULLIF(TRIM(f.phonenumber), '') ORDER BY f.account_id) AS phone_rank,
           ROW_NUMBER() OVER (PARTITION BY NULLIF(TRIM(f.email), '') ORDER BY f.account_id) AS email_rank
    FROM (SELECT ui.*, ROW_NUMBER() OVER (PARTITION BY ui.username ORDER BY ui.account_id) AS rn
          FROM userinfo ui WHERE ui.username IS NOT NULL) f
    WHERE f.rn = 1
) p;

SET @customer = (SELECT role_id FROM roles WHERE role_name = 'CUSTOMER');
SET @employee = (SELECT role_id FROM roles WHERE role_name = 'EMPLOYEE');
SET @manager  = (SELECT role_id FROM roles WHERE role_name = 'MANAGER');

INSERT INTO user_roles (user_id, role_id) SELECT user_id, @customer FROM users;

-- Employees. One who is also a customer (promoted) is the same person: they
-- keep their customer password and get the EMPLOYEE role as well.
INSERT INTO users (username, password_hash, full_name)
SELECT w.username, w.password, w.fullname FROM workerinfo w
WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.username = w.username);
INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.user_id, @employee FROM workerinfo w JOIN users u ON u.username = w.username;

-- Managers, the same way.
INSERT INTO users (username, password_hash, full_name)
SELECT CAST(b.username AS CHAR), b.password, b.fullname FROM bossinfo b
WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.username = CAST(b.username AS CHAR));
INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.user_id, @manager FROM bossinfo b JOIN users u ON u.username = CAST(b.username AS CHAR);

-- ================= accounts =================

INSERT INTO accounts (account_id, user_id, account_number, card_number, iban, cvv, expire_date, account_type, balance, statement_status, created_at)
SELECT ui.account_id, u.user_id, ui.accountnumber, ui.cardnumber, ui.iban, ui.cvv,
       COALESCE(LAST_DAY(STR_TO_DATE(CONCAT(REPLACE(TRIM(ui.expiredate), '/', '.'), '.01'), '%Y.%c.%d')),
                LAST_DAY(DATE_ADD(CURDATE(), INTERVAL 4 YEAR))),
       LOWER(TRIM(ui.accounttype)),
       ui.deposit,
       CASE LOWER(TRIM(ui.statement)) WHEN 'accepted' THEN 'active' WHEN 'pending' THEN 'pending' ELSE LOWER(TRIM(ui.statement)) END,
       COALESCE(STR_TO_DATE(ui.creationdate, '%Y-%m-%d %H:%i'), STR_TO_DATE(ui.creationdate, '%Y-%m-%d'), NOW())
FROM userinfo ui
JOIN users u ON u.username = CAST(ui.username AS CHAR)
WHERE ui.accountnumber IS NOT NULL AND ui.cardnumber IS NOT NULL AND ui.iban IS NOT NULL;

-- ================= the bank's own money =================

INSERT INTO bank_funds (id, balance) SELECT 1, COALESCE((SELECT balance FROM bankfunds WHERE id = 1), 0);

-- ================= transfers =================

INSERT INTO transactions (transaction_id, source_account_id, destination_account_id, transaction_type, amount, fee,
                          destination_name, destination_card, destination_iban, destination_bank_name, created_at, status)
SELECT t.transitionid,
       (SELECT a.account_id FROM accounts a WHERE a.account_number = t.sourceaccount OR a.card_number = t.sourcecard
            OR a.iban = t.sourceiban OR a.iban = CONCAT('IR', t.sourceiban) LIMIT 1),
       (SELECT a.account_id FROM accounts a WHERE a.account_number = t.destinationaccount OR a.card_number = t.destinationcard
            OR a.iban = t.destinationiban OR a.iban = CONCAT('IR', t.destinationiban) LIMIT 1),
       t.transitiontype, t.moneyamount, 0,
       t.destinationfullname, t.destinationcard, t.destinationiban, t.destinationbankname,
       COALESCE(t.time, NOW()), 'completed'
FROM transitions t;

-- ================= deposits =================
-- an old deposit belonged to a username: it is attached to that person's main account

INSERT INTO deposits (deposit_id, account_id, type, amount, duration_months, interest_rate, payout_type, currency, opened_at, maturity_date, status)
SELECT x.number, x.account_id, x.type, x.amount, x.months, x.rate, x.payment, x.currency, x.opened,
       IF(x.months IS NULL, NULL, DATE_ADD(x.opened, INTERVAL x.months MONTH)), 'active'
FROM (
    SELECT d.number, d.type, d.amount, d.payment,
           (SELECT a3.account_id FROM accounts a3 WHERE a3.user_id = u.user_id ORDER BY a3.statement_status = 'active' DESC, a3.account_type = 'checking' DESC, a3.account_id LIMIT 1) AS account_id,
           (CAST(NULLIF(REGEXP_REPLACE(d.duration, '[^0-9]', ''), '') AS UNSIGNED) * IF(LOWER(d.duration) LIKE '%year%', 12, 1)) AS months,
           CAST(NULLIF(REPLACE(TRIM(d.interest), '%', ''), '') AS DECIMAL(5,2)) AS rate,
           COALESCE(NULLIF(TRIM(d.currency), ''), 'IRR') AS currency,
           COALESCE(STR_TO_DATE(d.date, '%Y-%m-%d'), STR_TO_DATE(d.date, '%Y.%m.%d'), CURDATE()) AS opened
    FROM deposit d JOIN users u ON u.username = CAST(d.username AS CHAR)
) x
WHERE x.account_id IS NOT NULL;

-- ================= loans and their documents =================
-- The old app saved the interest number ("12") as the term. Those terms are
-- rebuilt from the rate, which was fixed per term: 12% 1 year, 16% 2, 18% 3, 20% 4, 22% 5.

INSERT INTO loans (loan_id, user_id, loan_amount, term_months, interest_rate, request_date, status)
SELECT l.loanid, u.user_id, l.loanamount,
       CASE WHEN LOWER(l.term) LIKE '%year%' OR LOWER(l.term) LIKE '%month%' THEN (CAST(NULLIF(REGEXP_REPLACE(l.term, '[^0-9]', ''), '') AS UNSIGNED) * IF(LOWER(l.term) LIKE '%year%', 12, 1))
            ELSE CASE REPLACE(TRIM(l.intrest), '%', '') WHEN '12' THEN 12 WHEN '16' THEN 24 WHEN '18' THEN 36
                                                       WHEN '20' THEN 48 WHEN '22' THEN 60 ELSE 12 END END,
       COALESCE(CAST(NULLIF(REPLACE(TRIM(l.intrest), '%', ''), '') AS DECIMAL(5,2)), 0),
       COALESCE(STR_TO_DATE(l.date, '%Y.%m.%d'), STR_TO_DATE(l.date, '%Y-%m-%d'), CURDATE()),
       CASE LOWER(TRIM(l.status)) WHEN 'final step' THEN 'final_step' ELSE LOWER(TRIM(l.status)) END
FROM loan l JOIN users u ON u.username = CAST(l.username AS CHAR);

INSERT INTO loan_documents (loan_id, document_type, content)
SELECT loanid, 'id', id FROM loan WHERE LENGTH(id) > 0 AND loanid IN (SELECT loan_id FROM loans)
UNION ALL SELECT loanid, 'employment', employment FROM loan WHERE LENGTH(employment) > 0 AND loanid IN (SELECT loan_id FROM loans)
UNION ALL SELECT loanid, 'collateral', collateral FROM loan WHERE LENGTH(collateral) > 0 AND loanid IN (SELECT loan_id FROM loans)
UNION ALL SELECT loanid, 'deed', deed FROM loan WHERE LENGTH(deed) > 0 AND loanid IN (SELECT loan_id FROM loans);

-- ================= bills and top-ups =================
-- the old app stored either the card number or the account number here

INSERT INTO bill_payments (payment_id, account_id, bill_type, bill_identifier, amount, tax, paid_by, paid_at, due_date, status)
SELECT x.transotionno, x.account_id, x.billtype, CAST(x.billid AS CHAR), x.billamount, ROUND(x.billamount * 0.09, 2), x.whopaid,
       COALESCE(x.payeddate, NOW()),
       COALESCE(STR_TO_DATE(x.duedate, '%Y.%m.%d'), STR_TO_DATE(x.duedate, '%Y-%m-%d')), 'paid'
FROM (
    SELECT b.*, COALESCE((SELECT a2.account_id FROM accounts a2 WHERE a2.account_number = b.accountno OR a2.card_number = b.accountno LIMIT 1),
                         (SELECT (SELECT a3.account_id FROM accounts a3 WHERE a3.user_id = u.user_id ORDER BY a3.statement_status = 'active' DESC, a3.account_type = 'checking' DESC, a3.account_id LIMIT 1) FROM users u WHERE u.username = CAST(b.username AS CHAR))) AS account_id
    FROM bills b
) x
WHERE x.account_id IS NOT NULL;

INSERT INTO mobile_charges (charge_id, account_id, phone_number, amount, tax, charged_at)
SELECT x.transotionno, x.account_id, x.telephoneno, x.amount, ROUND(x.amount * 0.09, 2), COALESCE(x.time, NOW())
FROM (
    SELECT c.*, COALESCE((SELECT a2.account_id FROM accounts a2 WHERE a2.account_number = c.accountno OR a2.card_number = c.accountno LIMIT 1),
                         (SELECT (SELECT a3.account_id FROM accounts a3 WHERE a3.user_id = u.user_id ORDER BY a3.statement_status = 'active' DESC, a3.account_type = 'checking' DESC, a3.account_id LIMIT 1) FROM users u WHERE u.username = CAST(c.username AS CHAR))) AS account_id
    FROM charge c
) x
WHERE x.account_id IS NOT NULL;

-- ================= cheques =================
-- an old cheque named its writer by full name: it is attached to that person's main account

INSERT INTO checks (check_id, issuer_account_id, recipient_name, amount, issue_date, due_date, status)
SELECT x.checknumber, x.account_id, x.recipientdate, x.checkamount, DATE(COALESCE(x.date, NOW())), DATE(COALESCE(x.duedate, x.date, NOW())), LOWER(TRIM(x.status))
FROM (
    SELECT c.*, (SELECT a.account_id FROM accounts a JOIN users u ON u.user_id = a.user_id WHERE u.full_name = c.issuername
                 ORDER BY a.statement_status = 'active' DESC, a.account_type = 'checking' DESC, a.account_id LIMIT 1) AS account_id
    FROM `check` c
) x
WHERE x.account_id IS NOT NULL;

-- ================= keep the old tables, out of the way =================

RENAME TABLE userinfo TO legacy_userinfo, workerinfo TO legacy_workerinfo, bossinfo TO legacy_bossinfo,
             transitions TO legacy_transitions, deposit TO legacy_deposit, loan TO legacy_loan, bills TO legacy_bills,
             charge TO legacy_charge, `check` TO legacy_check, bankfunds TO legacy_bankfunds;

SET SESSION sql_mode = @saved_sql_mode;
SET SQL_SAFE_UPDATES = 1;

-- ================= report: what moved, and anything that did not =================

SELECT 'customer rows (old) -> users' AS what, (SELECT COUNT(DISTINCT username) FROM legacy_userinfo) AS before_count, (SELECT COUNT(*) FROM user_roles WHERE role_id = @customer) AS after_count
UNION ALL SELECT 'accounts', (SELECT COUNT(*) FROM legacy_userinfo), (SELECT COUNT(*) FROM accounts)
UNION ALL SELECT 'employees', (SELECT COUNT(*) FROM legacy_workerinfo), (SELECT COUNT(*) FROM user_roles WHERE role_id = @employee)
UNION ALL SELECT 'managers', (SELECT COUNT(*) FROM legacy_bossinfo), (SELECT COUNT(*) FROM user_roles WHERE role_id = @manager)
UNION ALL SELECT 'transfers', (SELECT COUNT(*) FROM legacy_transitions), (SELECT COUNT(*) FROM transactions)
UNION ALL SELECT 'deposits', (SELECT COUNT(*) FROM legacy_deposit), (SELECT COUNT(*) FROM deposits)
UNION ALL SELECT 'loans', (SELECT COUNT(*) FROM legacy_loan), (SELECT COUNT(*) FROM loans)
UNION ALL SELECT 'bills', (SELECT COUNT(*) FROM legacy_bills), (SELECT COUNT(*) FROM bill_payments)
UNION ALL SELECT 'top-ups', (SELECT COUNT(*) FROM legacy_charge), (SELECT COUNT(*) FROM mobile_charges)
UNION ALL SELECT 'cheques', (SELECT COUNT(*) FROM legacy_check), (SELECT COUNT(*) FROM checks)
UNION ALL SELECT 'total balance', (SELECT SUM(deposit) FROM legacy_userinfo), (SELECT SUM(balance) FROM accounts)
UNION ALL SELECT 'customers whose national ID, phone or email was taken by someone else (left empty)', 0,
          (SELECT COUNT(*) FROM users u JOIN legacy_userinfo l ON CAST(l.username AS CHAR) = u.username
           WHERE (u.national_id IS NULL AND NULLIF(TRIM(l.id), '') IS NOT NULL)
              OR (u.phone_number IS NULL AND NULLIF(TRIM(l.phonenumber), '') IS NOT NULL)
              OR (u.email IS NULL AND NULLIF(TRIM(l.email), '') IS NOT NULL))
UNION ALL SELECT 'transfers whose source account was not found (kept, without a source)', 0,
          (SELECT COUNT(*) FROM transactions WHERE source_account_id IS NULL);
