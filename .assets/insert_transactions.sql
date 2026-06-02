-- Get any existing user and their account
SET @user_id = (SELECT id FROM users LIMIT 1);
SET @account_id = (SELECT id FROM accounts WHERE user_id = @user_id LIMIT 1);

-- Get any expense category and any income category
SET @expense_category = (SELECT id FROM categories WHERE type = 'EXPENSE' LIMIT 1);
SET @income_category = (SELECT id FROM categories WHERE type = 'INCOME' LIMIT 1);

-- Insert test transactions (simplified)
INSERT INTO transactions (user_id, account_id, type, category_id, amount, description, date, created_at, updated_at)
SELECT
    @user_id,
    @account_id,
    type,
    CASE WHEN type = 'EXPENSE' THEN @expense_category ELSE @income_category END,
    amount,
    description,
    date,
    NOW(),
    NOW()
FROM (
         SELECT 'EXPENSE' AS type, 125.50 AS amount, 'Groceries at supermarket' AS description, '2025-05-27' AS date UNION ALL
         SELECT 'EXPENSE', 45.30, 'Lunch at cafe', '2025-05-27' UNION ALL
         SELECT 'EXPENSE', 89.90, 'Cinema ticket', '2025-05-26' UNION ALL
         SELECT 'EXPENSE', 234.00, 'Weekly groceries', '2025-05-26' UNION ALL
         SELECT 'EXPENSE', 2500.00, 'Rent payment', '2025-05-25' UNION ALL
         SELECT 'EXPENSE', 320.50, 'Dinner with friends', '2025-05-25' UNION ALL
         SELECT 'EXPENSE', 150.00, 'Taxi ride', '2025-05-24' UNION ALL
         SELECT 'EXPENSE', 67.80, 'Bread and milk', '2025-05-24' UNION ALL
         SELECT 'EXPENSE', 550.00, 'Spotify subscription', '2025-05-23' UNION ALL
         SELECT 'EXPENSE', 180.00, 'Pizza', '2025-05-23' UNION ALL
         SELECT 'INCOME', 50000.00, 'Salary for May', '2025-05-20' UNION ALL
         SELECT 'INCOME', 5000.00, 'Freelance project', '2025-05-18' UNION ALL
         SELECT 'INCOME', 2000.00, 'Consulting fee', '2025-05-15' UNION ALL
         SELECT 'INCOME', 1000.00, 'Cashback reward', '2025-05-10'
     ) AS data;

SELECT CONCAT('Inserted test transactions for user_id=', @user_id, ' account_id=', @account_id) AS result;