-- =====================================================================
-- WA Bank database, for a new installation.
--   mysql -u root -p -e "CREATE DATABASE userdetail"
--   mysql -u root -p userdetail < database/schema.sql
-- A database from an earlier version is upgraded with the migrations in
-- database/migrations instead (001, 002, then 003).
-- =====================================================================

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
);


CREATE TABLE roles (
    role_id INT NOT NULL AUTO_INCREMENT,
    role_name VARCHAR(50) NOT NULL,

    PRIMARY KEY (role_id),

    UNIQUE KEY uq_roles_role_name (role_name)
);

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
);


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
);


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
);


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
);


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
);


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
);


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
);


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
);


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
);


-- The bank's own money: fees and taxes come in, salaries go out. One row.
CREATE TABLE bank_funds (
    id TINYINT NOT NULL,
    balance DECIMAL(15,2) NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT chk_bank_funds_single_row CHECK (id = 1),
    CONSTRAINT chk_bank_funds_not_negative CHECK (balance >= 0)
);


INSERT INTO bank_funds (id, balance) VALUES (1, 0.00);
