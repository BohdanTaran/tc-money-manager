package org.tc.mtracker.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.tc.mtracker.common.file.ObjectStorageKeys;
import org.tc.mtracker.currency.CurrencyCode;
import org.tc.mtracker.transaction.TransactionRepository;
import org.tc.mtracker.transaction.recurring.RecurringTransactionRepository;
import org.tc.mtracker.user.dto.RequestUpdateUserProfileDTO;
import org.tc.mtracker.user.dto.ResponseUserDTO;
import org.tc.mtracker.user.dto.UserMapper;
import org.tc.mtracker.utils.S3Service;
import org.tc.mtracker.utils.exceptions.UserNotFoundException;
import org.tc.mtracker.utils.exceptions.UserUpdateProfileException;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionRepository transactionRepository;
    private final RecurringTransactionRepository recurringTransactionRepository;

    public User getCurrentAuthenticatedUser(Authentication auth) {
        return getCurrentAuthenticatedUser(auth.getName());
    }

    public ResponseUserDTO getUser(String currentUserEmail) {
        User user = getCurrentAuthenticatedUser(currentUserEmail);
        log.debug("Loading user profile for email={}", currentUserEmail);
        return userMapper.toDto(user, generateAvatarUrl(user));
    }

    public User getCurrentAuthenticatedUser(String currentUserEmail) {
        return userRepository.findByEmail(currentUserEmail).orElseThrow(
                () -> {
                    log.warn("Authenticated user record not found for email={}", currentUserEmail);
                    return new UserNotFoundException("User not found.");
                }
        );
    }

    @Transactional
    public ResponseUserDTO updateProfile(RequestUpdateUserProfileDTO dto, MultipartFile avatar, Long userId) {
        User user = getUserById(userId);
        if (isCurrencyChangeRequested(dto, user) && userHasFinancialActivity(userId)) {
            throw new UserUpdateProfileException("Cannot update currency while user has financial activity.");
        }
        Boolean deleteAvatar = dto == null ? null : dto.deleteAvatar();
        handleUserAvatar(user, avatar, deleteAvatar);
        userMapper.updateEntityFromDto(dto, user);
        userRepository.save(user);
        String avatarUrl = generateAvatarUrl(user);
        log.info("User with id {} is updated successfully!", user.getId());
        return userMapper.toDto(user, avatarUrl);
    }

    private void handleUserAvatar(User user, MultipartFile avatar, Boolean deleteAvatar) {

        if (Boolean.TRUE.equals(deleteAvatar) && user.getAvatarId() != null) {
            s3Service.deleteFile(user.getAvatarId());
            user.setAvatarId(null);
        } else if (avatar != null && !avatar.isEmpty()) {
            s3Service.deleteFile(user.getAvatarId());
            uploadAvatar(avatar, user);
        }
    }

    private static boolean isCurrencyChangeRequested(RequestUpdateUserProfileDTO dto, User user) {
        return dto != null
                && dto.currencyCode() != null
                && isDifferentCurrency(dto.currencyCode(), user.getCurrencyCode());
    }

    private static boolean isDifferentCurrency(CurrencyCode requestedCurrency, CurrencyCode currentCurrency) {
        return requestedCurrency != currentCurrency;
    }

    private String generateAvatarUrl(User user) {
        return user.getAvatarId() != null ? s3Service.generatePresignedUrl(user.getAvatarId()) : null;
    }

    private void uploadAvatar(MultipartFile avatar, User user) {
        String avatarId = user.getAvatarId();
        if (avatarId == null) {
            avatarId = ObjectStorageKeys.newAvatarKey();
            user.setAvatarId(avatarId);
        }
        s3Service.saveFile(avatarId, avatar);
        log.info("Avatar uploaded successfully for userId={} avatarId={}", user.getId(), avatarId);
    }


    public User getUserById(long userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new UserNotFoundException(
                        (String.format("User with id#%d not found", userId)
                        )));
    }

    @Transactional
    public void deleteUserWithAllData(Long userId) {
        User user = getUserById(userId);
        log.info("Deleting user: {} (id={})", user.getEmail(), userId);

        List<Long> transactionIds = jdbcTemplate.queryForList(
                """
                        SELECT t.id FROM transactions t 
                                                INNER JOIN accounts a ON a.id = t.account_id
                                                WHERE a.user_id = ?
                        """,
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
                "DELETE t FROM transactions t " +
                        "INNER JOIN accounts a ON a.id = t.account_id " +
                        "WHERE a.user_id = ?",
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

    private boolean userHasFinancialActivity(long userId) {
        return transactionRepository.existsByUserIdAndDeletedAtIsNull(userId)
                || recurringTransactionRepository.existsByUserId(userId);
    }

}
