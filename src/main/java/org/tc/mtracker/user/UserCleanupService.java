package org.tc.mtracker.user;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tc.mtracker.utils.exceptions.UserNotFoundException;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCleanupService {

    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void deleteUserWithAllData(Long userId) {
        User user = getUserById(userId);
        log.info("Deleting user: {} (id={})", user.getEmail(), userId);

        List<Long> transactionIds = jdbcTemplate.queryForList(
                """
                        SELECT t.id FROM transactions t
                                                INNER JOIN accounts a ON a.id = t.account_id
                                                WHERE a.user_id = ?
                       \s""",
                Long.class,
                userId
        );

        if (!transactionIds.isEmpty()) {
            String placeholders = transactionIds.stream()
                    .map(id -> "?")
                    .collect(Collectors.joining(","));

            jdbcTemplate.update(
                    "DELETE FROM receipt_images WHERE transaction_id IN (" + placeholders + ")",
                    transactionIds.toArray()
            );
        }

        jdbcTemplate.update(
                "UPDATE transactions SET recurring_transaction_id = NULL WHERE user_id = ?",
                userId
        );

        jdbcTemplate.update(
                "DELETE FROM recurring_transactions WHERE user_id = ?",
                userId
        );

        jdbcTemplate.update(
                "UPDATE users SET default_account_id = NULL WHERE id = ?",
                userId
        );

        jdbcTemplate.update(
                """
                        DELETE t FROM transactions t
                                    INNER JOIN accounts a ON a.id = t.account_id
                                    WHERE a.user_id = ?
                        """,
                userId
        );

        jdbcTemplate.update(
                "DELETE FROM accounts WHERE user_id = ?",
                userId
        );

        jdbcTemplate.update(
                "DELETE FROM categories WHERE user_id = ?",
                userId
        );

        jdbcTemplate.update(
                "DELETE FROM refresh_tokens WHERE user_id = ?",
                userId
        );

        int deleted = jdbcTemplate.update(
                "DELETE FROM users WHERE id = ?",
                userId
        );

        if (deleted == 0) {
            throw new UserNotFoundException("User not found with id: " + userId);
        }
        log.info("User {} successfully deleted", userId);
    }


    private User getUserById(long userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new UserNotFoundException(
                        (String.format("User with id#%d not found", userId)
                        )));
    }


}
