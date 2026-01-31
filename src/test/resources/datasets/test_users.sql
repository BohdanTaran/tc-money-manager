-- Clean table
DELETE FROM users;

-- Password for all users is "12345678"
-- Hash: $2a$12$1b5AfGkfkGpDREqtic/Mh.g.ilnMjh08WF3ihHm3wp/5SJtR7nmjC

INSERT INTO users (full_name, email, password, is_activated, currency_code, created_at, updated_at)
VALUES
(
    'Active User',
    'test@gmail.com',
    '$2a$12$1b5AfGkfkGpDREqtic/Mh.g.ilnMjh08WF3ihHm3wp/5SJtR7nmjC',
    true, 'USD', NOW(), NOW()
),
(
    'Inactive User',
    'inactive@gmail.com',
    '$2a$12$1b5AfGkfkGpDREqtic/Mh.g.ilnMjh08WF3ihHm3wp/5SJtR7nmjC',
    false, 'EUR', NOW(), NOW()
),
(
    'Admin User',
    'admin@mtracker.com',
    '$2a$12$1b5AfGkfkGpDREqtic/Mh.g.ilnMjh08WF3ihHm3wp/5SJtR7nmjCW',
    true, 'GBP', NOW(), NOW()
);