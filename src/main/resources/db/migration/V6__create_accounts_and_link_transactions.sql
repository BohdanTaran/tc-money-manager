CREATE TABLE accounts
(
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT         NOT NULL,
    balance DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    CONSTRAINT fk_account_user FOREIGN KEY (user_id) REFERENCES users (id)
);

ALTER TABLE users
    ADD COLUMN default_account_id BIGINT NULL,
    ADD CONSTRAINT fk_user_default_account FOREIGN KEY (default_account_id) REFERENCES accounts (id);

ALTER TABLE transactions
    ADD COLUMN account_id BIGINT NULL,
    ADD CONSTRAINT fk_transaction_account FOREIGN KEY (account_id) REFERENCES accounts (id);

INSERT INTO accounts (user_id, balance)
SELECT u.id, 0.00
FROM users u;

UPDATE users u
    JOIN accounts a
ON a.user_id = u.id
SET u.default_account_id = a.id
WHERE u.default_account_id IS NULL;

UPDATE transactions t
    JOIN users u
ON u.id = t.user_id
SET t.account_id = u.default_account_id
WHERE t.account_id IS NULL;

ALTER TABLE transactions
    MODIFY COLUMN account_id BIGINT NOT NULL;
