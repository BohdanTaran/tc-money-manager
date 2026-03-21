CREATE TABLE receipt_images
(
    image_uuid     BINARY(16) PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    CONSTRAINT fk_receipt_transaction FOREIGN KEY (transaction_id) REFERENCES transactions (id)
)