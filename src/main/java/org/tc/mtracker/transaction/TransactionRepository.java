package org.tc.mtracker.transaction;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.tc.mtracker.category.Category;
import org.tc.mtracker.common.enums.TransactionType;
import org.tc.mtracker.transaction.dto.TransactionQueryParams;
import org.tc.mtracker.transaction.recurring.RecurringTransaction;
import org.tc.mtracker.user.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    @Query("""
                SELECT t FROM Transaction t
                WHERE t.id = :id
                AND t.user = :user
                AND t.deletedAt IS NULL
            """)
    Optional<Transaction> findActiveByIdAndUser(@Param("id") Long id, @Param("user") User user);

    @Query("""
                SELECT t FROM Transaction t
                WHERE t.user = :user
                AND t.deletedAt IS NULL
                AND (:accountId IS NULL OR t.account.id = :accountId)
                AND (:categoryId IS NULL OR t.category.id = :categoryId)
                AND (:type IS NULL OR t.type = :type)
                AND (:dateFrom IS NULL OR t.date >= :dateFrom)
                AND (:dateTo IS NULL OR t.date <= :dateTo)
                ORDER BY t.date DESC, t.createdAt DESC, t.id DESC
            """)
    List<Transaction> findAllByUserAndFilters(
            @Param("user") User user,
            @Param("accountId") Long accountId,
            @Param("categoryId") Long categoryId,
            @Param("type") TransactionType type,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo
    );

    long countByUserAndCategory(User user, Category category);

    List<Transaction> findAllByRecurringTransactionAndDateGreaterThanEqualOrderByDateAscIdAsc(
            RecurringTransaction recurringTransaction,
            LocalDate date
    );

    @Modifying
    @Query("""
                UPDATE Transaction t
                SET t.category = :replacementCategory
                WHERE t.user = :user
                AND t.category = :sourceCategory
            """)
    int reassignCategory(
            @Param("user") User user,
            @Param("sourceCategory") Category sourceCategory,
            @Param("replacementCategory") Category replacementCategory
    );

    @Query(value = """
        SELECT t.id
        FROM transactions t
        WHERE t.user_id = :#{#params.userId}
            AND (:#{#params.accountId} IS NULL OR t.account_id = :#{#params.accountId})
            AND (:#{#params.categoryId} IS NULL OR t.category_id = :#{#params.categoryId})
            AND (:#{#params.type} IS NULL OR t.type = :#{#params.type})
            AND (:#{#params.dateFrom} IS NULL OR t.date >= :#{#params.dateFrom})
            AND (:#{#params.dateTo} IS NULL OR t.date <= :#{#params.dateTo})
            AND (
                :#{#params.cursorDate} IS NULL
                OR t.date < :#{#params.cursorDate}
                OR (t.date = :#{#params.cursorDate} AND t.id < :#{#params.cursorId})
            )
        ORDER BY t.date DESC, t.id DESC
        LIMIT :#{#params.limit}
        """, nativeQuery = true)
    List<Long> findTransactionIdsWithCursor(@Param("params") TransactionQueryParams params);

    @EntityGraph(attributePaths = {"category", "receipts"})
    @Query("""
             SELECT t FROM Transaction t
             WHERE t.id IN :ids
             ORDER BY t.date DESC, t.id DESC
            """)
    List<Transaction> findAllByIdInOrderByDateDescIdDesc(@Param("ids") List<Long> ids);

    boolean existsByUserIdAndDeletedAtIsNull(Long userId);
}
