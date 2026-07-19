-- =====================================================
-- Скрипт для заполнения тестовыми данными
-- Пароль для всех пользователей: test123
-- =====================================================

-- =====================================================
-- 1. Создаем 10 пользователей
-- =====================================================
INSERT INTO users (full_name, password, email, currency_code, is_activated, created_at, updated_at)
VALUES
    ('Олексій Коваленко', '$2a$10$BY31DynXi4ntIkOquW5xduiFcw6VhRlfGX1CLkQNQqupErsblqBHy', 'alex.kovalenko@example.com', 'USD', TRUE, DATE_SUB(NOW(), INTERVAL 30 DAY), NOW()),
    ('Марія Петренко', '$2a$10$BY31DynXi4ntIkOquW5xduiFcw6VhRlfGX1CLkQNQqupErsblqBHy', 'maria.petrenko@example.com', 'EUR', TRUE, DATE_SUB(NOW(), INTERVAL 28 DAY), NOW()),
    ('Іван Шевченко', '$2a$10$BY31DynXi4ntIkOquW5xduiFcw6VhRlfGX1CLkQNQqupErsblqBHy', 'ivan.shevchenko@example.com', 'USD', TRUE, DATE_SUB(NOW(), INTERVAL 25 DAY), NOW()),
    ('Олена Бондаренко', '$2a$10$BY31DynXi4ntIkOquW5xduiFcw6VhRlfGX1CLkQNQqupErsblqBHy', 'olena.bondarenko@example.com', 'UAH', TRUE, DATE_SUB(NOW(), INTERVAL 22 DAY), NOW()),
    ('Андрій Мельник', '$2a$10$BY31DynXi4ntIkOquW5xduiFcw6VhRlfGX1CLkQNQqupErsblqBHy', 'andriy.melnyk@example.com', 'USD', TRUE, DATE_SUB(NOW(), INTERVAL 20 DAY), NOW()),
    ('Наталія Коваль', '$2a$10$BY31DynXi4ntIkOquW5xduiFcw6VhRlfGX1CLkQNQqupErsblqBHy', 'natalia.koval@example.com', 'EUR', TRUE, DATE_SUB(NOW(), INTERVAL 18 DAY), NOW()),
    ('Сергій Лисенко', '$2a$10$BY31DynXi4ntIkOquW5xduiFcw6VhRlfGX1CLkQNQqupErsblqBHy', 'sergiy.lysenko@example.com', 'USD', TRUE, DATE_SUB(NOW(), INTERVAL 15 DAY), NOW()),
    ('Ірина Савченко', '$2a$10$BY31DynXi4ntIkOquW5xduiFcw6VhRlfGX1CLkQNQqupErsblqBHy', 'iryna.savchenko@example.com', 'UAH', TRUE, DATE_SUB(NOW(), INTERVAL 12 DAY), NOW()),
    ('Михайло Гриценко', '$2a$10$BY31DynXi4ntIkOquW5xduiFcw6VhRlfGX1CLkQNQqupErsblqBHy', 'mykhailo.grytsenko@example.com', 'USD', TRUE, DATE_SUB(NOW(), INTERVAL 8 DAY), NOW()),
    ('Тетяна Романенко', '$2a$10$BY31DynXi4ntIkOquW5xduiFcw6VhRlfGX1CLkQNQqupErsblqBHy', 'tetiana.romanenko@example.com', 'EUR', TRUE, DATE_SUB(NOW(), INTERVAL 5 DAY), NOW());

-- =====================================================
-- 2. Создаем аккаунты для пользователей (начальный баланс от 1000 до 20000)
-- =====================================================
INSERT INTO accounts (user_id, balance, created_at, updated_at)
SELECT
    u.id,
    ROUND(1000 + RAND() * 19000, 2),
    u.created_at,
    NOW()
FROM users u;

-- =====================================================
-- 3. Устанавливаем default_account_id для пользователей
-- =====================================================
UPDATE users u
SET default_account_id = (
    SELECT a.id
    FROM accounts a
    WHERE a.user_id = u.id
    LIMIT 1
);

-- =====================================================
-- 4. Добавляем пользовательские категории
-- =====================================================
INSERT INTO categories (name, type, icon, status, user_id, scope, created_at, updated_at)
SELECT
    cat.name,
    cat.type,
    cat.icon,
    'ACTIVE',
    u.id,
    'USER',
    NOW(),
    NOW()
FROM users u
         CROSS JOIN (
    SELECT 'FREELANCE' AS name, 'INCOME' AS type, 'BRIEFCASE' AS icon
    UNION SELECT 'INVESTMENTS', 'INCOME', 'TREND_UP'
    UNION SELECT 'BONUS', 'INCOME', 'GIFT'
    UNION SELECT 'RENTAL_INCOME', 'INCOME', 'RECEIPT'
    UNION SELECT 'ENTERTAINMENT', 'EXPENSE', 'COINS'
    UNION SELECT 'UTILITIES', 'EXPENSE', 'WALLET'
    UNION SELECT 'EDUCATION', 'EXPENSE', 'DATABASE'
    UNION SELECT 'TRANSPORT', 'EXPENSE', 'DOCK'
    UNION SELECT 'SHOPPING', 'EXPENSE', 'AWARD'
    UNION SELECT 'HEALTHCARE', 'EXPENSE', 'TROPHY'
) cat;

-- =====================================================
-- 5. Создаем транзакции для каждого пользователя (50-100 штук)
-- =====================================================

-- Создаем временную таблицу с описаниями
CREATE TEMPORARY TABLE temp_descriptions (
                                             id INT AUTO_INCREMENT PRIMARY KEY,
                                             description_en VARCHAR(255),
                                             description_uk VARCHAR(255)
);

INSERT INTO temp_descriptions (description_en, description_uk) VALUES
                                                                   -- Доходы (10 шт)
                                                                   ('Monthly salary payment', 'Щомісячна зарплата'),
                                                                   ('Freelance project completion', 'Завершення фріланс-проекту'),
                                                                   ('Investment dividend received', 'Отримано дивіденди від інвестицій'),
                                                                   ('Quarterly performance bonus', 'Квартальна премія'),
                                                                   ('Consulting services', 'Консалтингові послуги'),
                                                                   ('Part-time job payment', 'Оплата підробітку'),
                                                                   ('Refund from online store', 'Повернення коштів з інтернет-магазину'),
                                                                   ('Interest from savings', 'Відсотки від накопичень'),
                                                                   ('Gift from relatives', 'Подарунок від родичів'),
                                                                   ('Sale of personal item', 'Продаж особистої речі'),

                                                                   -- Расходы (30 шт)
                                                                   ('Weekly groceries', 'Щотижневі продукти'),
                                                                   ('Monthly rent', 'Оренда за місяць'),
                                                                   ('Dinner at restaurant', 'Вечеря в ресторані'),
                                                                   ('Coffee shop visit', 'Візит до кав\'ярні'),
                                                                   ('Taxi ride', 'Поїздка на таксі'),
                                                                   ('Electricity bill', 'Платіж за електроенергію'),
                                                                   ('Water bill', 'Платіж за воду'),
                                                                   ('Gas bill', 'Платіж за газ'),
                                                                   ('Internet subscription', 'Інтернет-підписка'),
                                                                   ('Mobile phone bill', 'Рахунок за мобільний зв\'язок'),
                                                                   ('Streaming services', 'Стрімінгові сервіси'),
                                                                   ('Gym membership', 'Абонемент у спортзал'),
                                                                   ('Books and magazines', 'Книги та журнали'),
                                                                   ('Phone repair', 'Ремонт телефону'),
                                                                   ('Clothing purchase', 'Покупка одягу'),
                                                                   ('Fuel for car', 'Пальне для авто'),
                                                                   ('Dentist appointment', 'Прийом у стоматолога'),
                                                                   ('Car insurance', 'Страхування авто'),
                                                                   ('Food delivery', 'Доставка їжі'),
                                                                   ('Cinema tickets', 'Квитки в кіно'),
                                                                   ('Birthday gift', 'Подарунок на день народження'),
                                                                   ('Household supplies', 'Господарські товари'),
                                                                   ('Office supplies', 'Канцтовари'),
                                                                   ('Parking fee', 'Платіж за паркування'),
                                                                   ('Public transport', 'Громадський транспорт'),
                                                                   ('Healthcare products', 'Медичні товари'),
                                                                   ('Sport equipment', 'Спортивне обладнання'),
                                                                   ('Home renovation', 'Ремонт вдома'),
                                                                   ('Pet supplies', 'Товари для тварин'),
                                                                   ('Charity donation', 'Благодійний внесок');

-- Создаем хранимую процедуру для генерации транзакций
DELIMITER $$

DROP PROCEDURE IF EXISTS generate_transactions$$
CREATE PROCEDURE generate_transactions()
BEGIN
    DECLARE v_user_id BIGINT;
    DECLARE v_account_id BIGINT;
    DECLARE v_category_id BIGINT;
    DECLARE v_type VARCHAR(16);
    DECLARE v_amount DECIMAL(14,2);
    DECLARE v_date DATE;
    DECLARE v_description VARCHAR(255);
    DECLARE v_desc_en VARCHAR(255);
    DECLARE v_desc_uk VARCHAR(255);
    DECLARE v_transaction_count INT;
    DECLARE v_i INT;
    DECLARE v_desc_id INT;
    DECLARE done INT DEFAULT FALSE;

    -- Курсор для всех пользователей
    DECLARE user_cursor CURSOR FOR
        SELECT u.id, a.id
        FROM users u
                 JOIN accounts a ON a.user_id = u.id;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN user_cursor;

    read_loop: LOOP
        FETCH user_cursor INTO v_user_id, v_account_id;
        IF done THEN
            LEAVE read_loop;
        END IF;

        -- Генерируем случайное количество транзакций от 50 до 100
        SET v_transaction_count = 50 + FLOOR(RAND() * 51);
        SET v_i = 0;

        WHILE v_i < v_transaction_count DO
                -- Выбираем случайную категорию пользователя
                SELECT id, type INTO v_category_id, v_type
                FROM categories
                WHERE user_id = v_user_id
                ORDER BY RAND()
                LIMIT 1;

                -- Сумма: доходы 100-3000, расходы 20-1000
                IF v_type = 'INCOME' THEN
                    SET v_amount = ROUND(100 + RAND() * 2900, 2);
                ELSE
                    SET v_amount = ROUND(20 + RAND() * 980, 2);
                END IF;

                -- Дата: последние 60 дней
                SET v_date = DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 60) DAY);

                -- Случайное описание (на английском или украинском)
                SET v_desc_id = 1 + FLOOR(RAND() * 40);
                SELECT description_en, description_uk INTO v_desc_en, v_desc_uk
                FROM temp_descriptions
                WHERE id = v_desc_id;

                -- Выбираем язык случайно
                IF RAND() < 0.5 THEN
                    SET v_description = CONCAT(v_desc_en, ' #', FLOOR(1000 + RAND() * 9000));
                ELSE
                    SET v_description = CONCAT(v_desc_uk, ' #', FLOOR(1000 + RAND() * 9000));
                END IF;

                -- Вставляем транзакцию
                INSERT INTO transactions (
                    account_id,
                    category_id,
                    amount,
                    type,
                    date,
                    description,
                    created_at,
                    updated_at
                ) VALUES (
                             v_account_id,
                             v_category_id,
                             v_amount,
                             v_type,
                             v_date,
                             v_description,
                             NOW(),
                             NOW()
                         );

                SET v_i = v_i + 1;
            END WHILE;

    END LOOP;

    CLOSE user_cursor;
END$$

DELIMITER ;

-- Запускаем генерацию
CALL generate_transactions();

-- Удаляем процедуру и временную таблицу
DROP PROCEDURE IF EXISTS generate_transactions;
DROP TEMPORARY TABLE temp_descriptions;

-- =====================================================
-- 6. Проверяем результат
-- =====================================================
SELECT
    u.email,
    COUNT(DISTINCT c.id) as categories_count,
    COUNT(t.id) as transactions_count,
    ROUND(AVG(t.amount), 2) as avg_amount,
    ROUND(SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE 0 END), 2) as total_income,
    ROUND(SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount ELSE 0 END), 2) as total_expense
FROM users u
         LEFT JOIN categories c ON c.user_id = u.id
         LEFT JOIN accounts a ON a.user_id = u.id
         LEFT JOIN transactions t ON t.account_id = a.id
GROUP BY u.id, u.email
ORDER BY u.id;

-- Посмотрим несколько примеров транзакций
SELECT
    u.email,
    t.date,
    t.type,
    t.amount,
    t.description,
    c.name as category
FROM transactions t
         JOIN accounts a ON a.id = t.account_id
         JOIN users u ON u.id = a.user_id
         JOIN categories c ON c.id = t.category_id
ORDER BY RAND()
LIMIT 20;