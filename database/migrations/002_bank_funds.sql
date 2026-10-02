-- =====================================================================
-- 002: the bank's own money moves into the database.
-- It was kept in income.csv, outside any transaction: a fee could be taken
-- from a customer and never reach the bank, and two operations at once
-- overwrote each other. Now it changes in the same transaction as the
-- customer's money.
--
-- The starting value below is what income.csv held when this was written.
-- If yours differs, put your number in instead before running this.
-- =====================================================================
CREATE TABLE IF NOT EXISTS bankfunds (
  id TINYINT NOT NULL PRIMARY KEY,
  balance DECIMAL(20,2) NOT NULL,
  CONSTRAINT chk_bankfunds_single_row CHECK (id = 1),
  CONSTRAINT chk_bankfunds_not_negative CHECK (balance >= 0)
);
INSERT INTO bankfunds (id, balance) VALUES (1, 5827500.00)
  ON DUPLICATE KEY UPDATE balance = balance;
