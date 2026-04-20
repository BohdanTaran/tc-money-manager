package org.tc.mtracker.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.tc.mtracker.account.Account;
import org.tc.mtracker.account.AccountRepository;
import org.tc.mtracker.category.Category;
import org.tc.mtracker.category.CategoryService;
import org.tc.mtracker.category.enums.CategoryStatus;
import org.tc.mtracker.common.enums.TransactionType;
import org.tc.mtracker.user.User;
import org.tc.mtracker.utils.exceptions.AccountNotFoundException;
import org.tc.mtracker.utils.exceptions.CategoryIsNotActiveException;
import org.tc.mtracker.utils.exceptions.InvalidTransactionDateException;
import org.tc.mtracker.utils.exceptions.MoneyFlowTypeMismatchException;

import java.time.Clock;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionValidationService {

    private final AccountRepository accountRepository;
    private final CategoryService categoryService;
    private final Clock clock;

    public Account resolveAccount(User user, Long accountId) {
        if (accountId == null) {
            Account defaultAccount = user.getDefaultAccount();
            if (defaultAccount == null) {
                log.warn("Transaction request rejected: default account missing for userId={}", user.getId());
                throw new AccountNotFoundException("Current user does not have default account");
            }
            return defaultAccount;
        }

        return accountRepository.findByIdAndUser(accountId, user)
                .orElseThrow(() -> {
                    log.warn("Transaction request rejected: account not found userId={} accountId={}", user.getId(), accountId);
                    return new AccountNotFoundException("Account with id %d not found".formatted(accountId));
                });
    }

    public Category resolveActiveCategory(Long categoryId, User user) {
        Category category = categoryService.findAccessibleById(categoryId, user);
        if (category.getStatus() != CategoryStatus.ACTIVE) {
            log.warn("Transaction request rejected: category inactive userId={} categoryId={}",
                    user.getId(), category.getId());
            throw new CategoryIsNotActiveException("Category is not active.");
        }
        return category;
    }

    public void validateTransactionType(TransactionType type, Category category, User user) {
        if (category.getType() != type) {
            log.warn("Transaction request rejected: type mismatch userId={} categoryId={} categoryType={} transactionType={}",
                    user.getId(), category.getId(), category.getType(), type);
            throw new MoneyFlowTypeMismatchException("Category type does not match transaction type.");
        }
    }

    public void validateOneTimeTransactionDate(LocalDate date, User user) {
        LocalDate today = LocalDate.now(clock);
        if (date.isAfter(today)) {
            log.warn("One-time transaction rejected: future date userId={} transactionDate={} today={}",
                    user.getId(), date, today);
            throw new InvalidTransactionDateException("One-time transaction date cannot be in the future.");
        }
    }

    public void validateRecurringStartDate(LocalDate date, User user) {
        LocalDate today = LocalDate.now(clock);
        if (date.isBefore(today)) {
            log.warn("Recurring transaction rejected: past start date userId={} startDate={} today={}",
                    user.getId(), date, today);
            throw new InvalidTransactionDateException("Recurring transaction start date must be today or in the future.");
        }
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }
}
