-- =====================================================================
-- 001: fixes to the schema the app shipped with. Safe to run on a
-- database that already holds customers: nothing is deleted.
--
--   1. Every amount of money becomes DECIMAL(20,2) instead of text.
--      Text sorted "9000" above "10000", and the old code sometimes wrote
--      balances like "1.5E7". Values are cleaned (commas removed,
--      exponents expanded) before the column type changes.
--   2. userinfo and loan get a primary key. Loans were identified by
--      owner and amount, so approving one approved every loan with the
--      same amount.
--   3. CVV is no longer unique. It is three digits, so a unique CVV meant
--      at most 1000 customers, and sign-ups failing at random before that.
--   4. Password columns grow to 255 characters to hold hashes. Existing
--      plain-text passwords keep working and are hashed at the next sign-in.
--
-- Back up first:  mysqldump -u root -p userdetail > backup.sql
-- Then run:       mysql -u root -p userdetail < 001_fix_money_keys_and_passwords.sql
-- =====================================================================

-- MySQL Workbench refuses UPDATEs that touch every row ("safe update mode",
-- error 1175). Converting every amount is exactly that, so it is switched off
-- for this session only, and back on at the end.
SET SQL_SAFE_UPDATES = 0;

-- ---- 1. money: clean the text, then change the type ----
UPDATE userinfo    SET deposit     = CAST(REPLACE(REPLACE(deposit, ',', ''), ' ', '') + 0 AS DECIMAL(20,2));
UPDATE transitions SET moneyamount = CAST(REPLACE(REPLACE(moneyamount, ',', ''), ' ', '') + 0 AS DECIMAL(20,2));
UPDATE bills       SET billamount  = CAST(REPLACE(REPLACE(billamount, ',', ''), ' ', '') + 0 AS DECIMAL(20,2));
UPDATE charge      SET amount      = CAST(REPLACE(REPLACE(amount, ',', ''), ' ', '') + 0 AS DECIMAL(20,2));
UPDATE `check`     SET checkamount = CAST(REPLACE(REPLACE(checkamount, ',', ''), ' ', '') + 0 AS DECIMAL(20,2));
UPDATE deposit     SET amount      = CAST(REPLACE(REPLACE(amount, ',', ''), ' ', '') + 0 AS DECIMAL(20,2));
UPDATE loan        SET loanamount  = CAST(REPLACE(REPLACE(loanamount, ',', ''), ' ', '') + 0 AS DECIMAL(20,2));

ALTER TABLE userinfo    MODIFY deposit     DECIMAL(20,2) NOT NULL DEFAULT 0;
ALTER TABLE transitions MODIFY moneyamount DECIMAL(20,2) NOT NULL;
ALTER TABLE bills       MODIFY billamount  DECIMAL(20,2) NOT NULL;
ALTER TABLE charge      MODIFY amount      DECIMAL(20,2) NOT NULL;
ALTER TABLE `check`     MODIFY checkamount DECIMAL(20,2) NOT NULL;
ALTER TABLE deposit     MODIFY amount      DECIMAL(20,2) NOT NULL;
ALTER TABLE loan        MODIFY loanamount  DECIMAL(20,2) NOT NULL;

-- ---- 2. primary keys ----
ALTER TABLE userinfo ADD COLUMN account_id INT NOT NULL AUTO_INCREMENT FIRST, ADD PRIMARY KEY (account_id);
ALTER TABLE loan     ADD COLUMN loanid BIGINT NOT NULL AUTO_INCREMENT FIRST, ADD PRIMARY KEY (loanid);
CREATE INDEX idx_userinfo_username ON userinfo (username);
CREATE INDEX idx_loan_status ON loan (status);

-- ---- 3. CVV is not unique ----
ALTER TABLE userinfo DROP INDEX cvv_UNIQUE;

-- ---- 4. room for password hashes ----
ALTER TABLE userinfo   MODIFY password VARCHAR(255) NOT NULL;
ALTER TABLE workerinfo MODIFY password VARCHAR(255) NOT NULL;
ALTER TABLE bossinfo   MODIFY password VARCHAR(255) NOT NULL;

-- ---- a check constraint so no bug can ever make a balance negative ----
ALTER TABLE userinfo ADD CONSTRAINT chk_balance_not_negative CHECK (deposit >= 0);

SET SQL_SAFE_UPDATES = 1;
