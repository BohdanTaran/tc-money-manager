ALTER TABLE transactions
    ADD COLUMN recurring_transaction_id BIGINT,
    ADD CONSTRAINT fk_transaction_recurring_transaction
        FOREIGN KEY (recurring_transaction_id) REFERENCES recurring_transactions (id)
            ON DELETE SET NULL;