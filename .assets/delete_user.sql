START TRANSACTION;

SET @user_id = 6;

UPDATE users
SET default_account_id = NULL
WHERE id = @user_id;

DELETE t
FROM transactions t
    INNER JOIN accounts a ON a.id = t.account_id
WHERE a.user_id = @user_id;

DELETE FROM accounts
WHERE user_id = @user_id;

DELETE FROM users
WHERE id = @user_id;

COMMIT;