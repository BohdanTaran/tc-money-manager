CREATE INDEX idx_transactions_cursor_user_date_id
    ON transactions(user_id, date DESC, id DESC);

CREATE INDEX idx_transactions_account_user_date
    ON transactions(account_id, user_id, date DESC);

CREATE INDEX idx_transactions_category_user_date
    ON transactions(category_id, user_id, date DESC);

CREATE INDEX idx_transactions_type_user_date
    ON transactions(type, user_id, date DESC);