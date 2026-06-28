ALTER TABLE categories
    ADD COLUMN scope VARCHAR(16) NOT NULL;

INSERT INTO categories (name, type, icon, status, user_id, scope)
VALUES
    ('SALARY', 'INCOME', 'DOLLAR', 'ACTIVE', NULL, 'GLOBAL'),
    ('FREELANCE', 'INCOME', 'BRIEFCASE', 'ACTIVE', NULL, 'GLOBAL'),
    ('PRESENT', 'INCOME', 'GIFT', 'ACTIVE', NULL, 'GLOBAL');

INSERT INTO categories (name, type, icon, status, user_id, scope)
VALUES
    ('GROCERIES', 'EXPENSE', 'WALLET', 'ACTIVE', NULL, 'GLOBAL'),
    ('TRANSPORTATION', 'EXPENSE', 'DOCK', 'ACTIVE', NULL, 'GLOBAL'),
    ('DINING_OUT', 'EXPENSE', 'COINS', 'ACTIVE', NULL, 'GLOBAL'),
    ('HEALTH', 'EXPENSE', 'TREND_UP', 'ACTIVE', NULL, 'GLOBAL'),
    ('CLOTHING', 'EXPENSE', 'AWARD', 'ACTIVE', NULL, 'GLOBAL');