ALTER TABLE users
    ADD COLUMN default_account_id BIGINT NULL,
    ADD CONSTRAINT fk_user_default_account
        FOREIGN KEY (default_account_id)
            REFERENCES accounts (id) ON DELETE SET NULL;

CREATE INDEX idx_users_default_account ON users (default_account_id);