-- -----------------------------------------------------
INSERT INTO users (id, full_name, password, email, is_activated, currency_code, created_at, updated_at)
VALUES (1, 'Джон Сміт', '$2a$10$7X9JUFucS3d2Z0a2w0iLyuND6aXoUdvwFv/DbzOLfw7FBSz3PNrLG', 'ivan@example.com', TRUE,
        'UAH', NOW(), NOW()),
       (2, 'Юлій Цезарь', '$2a$10$7X9JUFucS3d2Z0a2w0iLyuND6aXoUdvwFv/DbzOLfw7FBSz3PNrLG', 'maria@example.com', TRUE,
        'USD', NOW(), NOW()),
       (3, 'Панас Мирний', '$2a$10$7X9JUFucS3d2Z0a2w0iLyuND6aXoUdvwFv/DbzOLfw7FBSz3PNrLG', 'alexey@example.com',
        FALSE, 'EUR', NOW(), NOW());

-- -----------------------------------------------------
INSERT INTO accounts (id, user_id, balance)
VALUES

(1, 1, 15000.50),
(2, 1, 500.75),

(3, 2, 1000.00),
(4, 2, 250.30),

(5, 3, 0.00);

INSERT INTO categories (id, name, type, icon, status, user_id, created_at, updated_at)
VALUES (1, 'Продукты', 'EXPENSE', 'shopping-cart', 'ACTIVE', 1, NOW(), NOW()),
       (2, 'Транспорт', 'EXPENSE', 'bus', 'ACTIVE', 1, NOW(), NOW()),
       (3, 'Зарплата', 'INCOME', 'dollar-sign', 'ACTIVE', 1, NOW(), NOW()),
       (4, 'Развлечения', 'EXPENSE', 'film', 'ACTIVE', 1, NOW(), NOW()),

       (5, 'Groceries', 'EXPENSE', 'shopping-cart', 'ACTIVE', 2, NOW(), NOW()),
       (6, 'Transport', 'EXPENSE', 'bus', 'ACTIVE', 2, NOW(), NOW()),
       (7, 'Salary', 'INCOME', 'dollar-sign', 'ACTIVE', 2, NOW(), NOW()),

       (8, 'Тестовая', 'EXPENSE', 'test', 'ACTIVE', 3, NOW(), NOW());


UPDATE users
SET default_account_id = CASE
                             WHEN id = 1 THEN 1
                             WHEN id = 2 THEN 3
                             WHEN id = 3 THEN 5
    END
WHERE id IN (1, 2, 3);

INSERT INTO transactions (user_id, category_id, account_id, amount, type, date, description, created_at, updated_at)
VALUES
(1, 3, 1, 50000.00, 'INCOME', '2024-01-10', 'Зарплата за январь', NOW(), NOW()),
(1, 3, 1, 50000.00, 'INCOME', '2024-02-10', 'Зарплата за февраль', NOW(), NOW()),

(1, 1, 1, 3500.50, 'EXPENSE', '2024-01-15', 'Покупка продуктов в Магните', NOW(), NOW()),
(1, 1, 1, 2800.00, 'EXPENSE', '2024-01-22', 'Продукты в Пятёрочке', NOW(), NOW()),
(1, 2, 1, 1500.00, 'EXPENSE', '2024-01-18', 'Проездной на месяц', NOW(), NOW()),
(1, 4, 2, 800.00, 'EXPENSE', '2024-01-25', 'Кино с друзьями', NOW(), NOW()),
(1, 1, 1, 4200.00, 'EXPENSE', '2024-02-05', 'Продукты', NOW(), NOW()),
(1, 2, 1, 300.00, 'EXPENSE', '2024-02-12', 'Такси до работы', NOW(), NOW()),
(1, 4, 2, 1200.00, 'EXPENSE', '2024-02-20', 'Ресторан', NOW(), NOW()),

(2, 7, 3, 3000.00, 'INCOME', '2024-01-05', 'Salary January', NOW(), NOW()),

(2, 5, 3, 350.50, 'EXPENSE', '2024-01-10', 'Walmart', NOW(), NOW()),
(2, 6, 4, 45.00, 'EXPENSE', '2024-01-15', 'Uber ride', NOW(), NOW()),
(2, 5, 3, 280.30, 'EXPENSE', '2024-01-20', 'Whole Foods', NOW(), NOW());


INSERT INTO recurring_transactions (id, user_id, account_id, category_id, amount, type, start_date, description,
                                    next_execution_date, interval_unit, created_at, updated_at)
VALUES (1, 1, 1, 3, 50000.00, 'INCOME', '2024-01-10', 'Ежемесячная зарплата', '2024-03-10', 'MONTHLY', NOW(), NOW()),
       (2, 1, 1, 1, 15000.00, 'EXPENSE', '2024-01-15', 'Ежемесячные продукты', '2024-03-15', 'MONTHLY', NOW(), NOW()),
       (3, 2, 3, 7, 3000.00, 'INCOME', '2024-01-05', 'Monthly Salary', '2024-03-05', 'MONTHLY', NOW(), NOW());


UPDATE transactions
SET recurring_transaction_id = 1
WHERE user_id = 1
  AND type = 'INCOME'
  AND amount = 50000.00;

UPDATE transactions
SET recurring_transaction_id = 2
WHERE user_id = 1
  AND type = 'EXPENSE'
  AND category_id = 1
  AND amount = 3500.50;


INSERT INTO receipt_images (image_uuid, transaction_id)
VALUES (UNHEX(REPLACE(UUID(), '-', '')), 3),
       (UNHEX(REPLACE(UUID(), '-', '')), 6),
       (UNHEX(REPLACE(UUID(), '-', '')), 10);
















# SELECT '=== Пользователи и их счета ===' as '';
# SELECT u.id, u.full_name, u.is_activated, COUNT(a.id) as accounts_count
# FROM users u
#          LEFT JOIN accounts a ON a.user_id = u.id
# GROUP BY u.id;
#
# SELECT '=== Транзакции по пользователям ===' as '';
# SELECT u.id, u.full_name, u.is_activated, COUNT(t.id) as transactions_count
# FROM users u
#          LEFT JOIN transactions t ON t.user_id = u.id
# GROUP BY u.id;
#
# SELECT '=== Все транзакции с деталями ===' as '';
# SELECT t.id,
#        u.full_name,
#        c.name as category,
#        a.balance,
#        t.amount,
#        t.type,
#        t.date,
#        t.description
# FROM transactions t
#          JOIN users u ON u.id = t.user_id
#          JOIN categories c ON c.id = t.category_id
#          JOIN accounts a ON a.id = t.account_id
# ORDER BY t.user_id, t.date;