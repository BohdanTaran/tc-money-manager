-- Clean table
DELETE FROM refresh_tokens;
DELETE FROM users;

-- Password for all users is "12345678"
-- Hash: $2a$12$1b5AfGkfkGpDREqtic/Mh.g.ilnMjh08WF3ihHm3wp/5SJtR7nmjC

INSERT INTO users (id, full_name, email, password, is_activated, currency_code, created_at, updated_at)
VALUES
    (1, 'Active User', 'test@gmail.com', '$2a$12$1b5AfGkfkGpDREqtic/Mh.g.ilnMjh08WF3ihHm3wp/5SJtR7nmjC', true, 'USD', NOW(), NOW()),
    (2, 'Inactive User', 'inactive@gmail.com', '$2a$12$1b5AfGkfkGpDREqtic/Mh.g.ilnMjh08WF3ihHm3wp/5SJtR7nmjC', false, 'EUR', NOW(), NOW()),
    (3, 'Admin User', 'admin@mtracker.com', '$2a$12$1b5AfGkfkGpDREqtic/Mh.g.ilnMjh08WF3ihHm3wp/5SJtR7nmjC', true, 'UAH', NOW(), NOW());

-- Add a valid refresh token for user 1 (Expires in 2030)
INSERT INTO refresh_tokens (id, token, expiry_date, user_id)
VALUES (1, 'existing-refresh-token-uuid', '2030-01-01 00:00:00', 1);