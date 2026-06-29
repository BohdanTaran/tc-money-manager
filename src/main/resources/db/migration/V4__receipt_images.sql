CREATE TABLE receipt_images
(
    image_uuid BINARY (16) PRIMARY KEY,
    transaction_id BIGINT   NOT NULL,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_receipt_transaction FOREIGN KEY (transaction_id)
        REFERENCES transactions (id) ON DELETE CASCADE,
    INDEX          idx_receipt_transaction_id(transaction_id)
);