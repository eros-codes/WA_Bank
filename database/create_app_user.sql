-- A MySQL user for the app with only what it needs: reading and writing data
-- in this one database. Run as root once, then put the same name and
-- password in your .env file. Using root from the app means any bug in the
-- app is a bug with full control of the MySQL server.
CREATE USER IF NOT EXISTS 'bank_app'@'localhost' IDENTIFIED BY 'change-me';
GRANT SELECT, INSERT, UPDATE, DELETE ON userdetail.* TO 'bank_app'@'localhost';
FLUSH PRIVILEGES;
