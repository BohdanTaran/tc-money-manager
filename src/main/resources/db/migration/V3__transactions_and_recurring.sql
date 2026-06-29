CREATE TABLE recurring_transactions
(
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id          BIGINT         NOT NULL,
    category_id         BIGINT         NOT NULL,
    amount              DECIMAL(14, 2) NOT NULL,
    type                VARCHAR(16)    NOT NULL,
    start_date          DATE           NOT NULL,
    description         VARCHAR(255),
    next_execution_date DATE           NOT NULL,
    interval_unit       VARCHAR(16)    NOT NULL,
    created_at          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_recurring_transaction_account FOREIGN KEY (account_id)
        REFERENCES accounts (id) ON DELETE CASCADE,
    CONSTRAINT fk_recurring_transaction_category FOREIGN KEY (category_id)
        REFERENCES categories (id) ON DELETE RESTRICT,
    INDEX idx_recurring_account_id (account_id),
    INDEX idx_recurring_next_execution (next_execution_date)
);

CREATE TABLE transactions
(
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id               BIGINT         NOT NULL,
    category_id              BIGINT         NOT NULL,
    amount                   DECIMAL(14, 2) NOT NULL,
    type                     VARCHAR(16)    NOT NULL,
    date                     DATE           NOT NULL,
    description              VARCHAR(255),
    recurring_transaction_id BIGINT         NULL,
    deleted_at               TIMESTAMP      NULL,
    created_at               TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_transaction_account FOREIGN KEY (account_id)
        REFERENCES accounts (id) ON DELETE CASCADE,
    CONSTRAINT fk_transaction_category FOREIGN KEY (category_id)
        REFERENCES categories (id) ON DELETE RESTRICT,
    CONSTRAINT fk_transaction_recurring FOREIGN KEY (recurring_transaction_id)
        REFERENCES recurring_transactions (id) ON DELETE SET NULL,

    INDEX idx_transactions_account_date (account_id, date DESC),
    INDEX idx_transactions_category_date (category_id, date DESC),
    INDEX idx_transactions_type_date (type, date DESC)
);