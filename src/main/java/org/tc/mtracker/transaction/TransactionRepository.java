package org.tc.mtracker.transaction;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.tc.mtracker.category.Category;
import org.tc.mtracker.common.enums.TransactionType;
import org.tc.mtracker.transaction.recurring.RecurringTransaction;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    @Query("""
                SELECT t FROM Transaction t
                WHERE t.id = :id
                AND t.account.user.id = :userId
                AND t.deletedAt IS NULL
            """)
    Optional<Transaction> findActiveByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);

    @Query("""
                SELECT t FROM Transaction t
                WHERE t.account.user.id = :userId
                AND t.deletedAt IS NULL
                AND (:accountId IS NULL OR t.account.id = :accountId)
                AND (:categoryId IS NULL OR t.category.id = :categoryId)
                AND (:type IS NULL OR t.type = :type)
                AND (:dateFrom IS NULL OR t.date >= :dateFrom)
                AND (:dateTo IS NULL OR t.date <= :dateTo)
                ORDER BY t.date DESC, t.createdAt DESC, t.id DESC
            """)
    List<Transaction> findAllByUserAndFilters(
            @Param("userId") Long userId,
            @Param("accountId") Long accountId,
            @Param("categoryId") Long categoryId,
            @Param("type") TransactionType type,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo
    );


    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.account.user.id = :userId AND t.category = :category")
    long countByUserAndCategory(@Param("userId") Long userId, @Param("category") Category category);

    List<Transaction> findAllByRecurringTransactionAndDateGreaterThanEqualOrderByDateAscIdAsc(
            RecurringTransaction recurringTransaction,
            LocalDate date
    );

    @Modifying
    @Query("""
                UPDATE Transaction t
                SET t.category = :replacementCategory
                WHERE t.account.user.id = :userId
                AND t.category = :sourceCategory
            """)
    void reassignCategory(
            @Param("userId") Long userId,
            @Param("sourceCategory") Category sourceCategory,
            @Param("replacementCategory") Category replacementCategory
    );

    @Query(value = """
            SELECT t.id
            FROM transactions t
            WHERE t.account_id IN (
                SELECT a.id FROM accounts a WHERE a.user_id = :userId
            )
            AND (:accountId IS NULL OR t.account_id = :accountId)
            AND (:categoryId IS NULL OR t.category_id = :categoryId)
            AND (:type IS NULL OR t.type = :type)
            AND (:dateFrom IS NULL OR t.date >= :dateFrom)
            AND (:dateTo IS NULL OR t.date <= :dateTo)
            AND (
                :cursorDate IS NULL
                OR t.date < :cursorDate
                OR (t.date = :cursorDate AND t.id < :cursorId)
            )
            ORDER BY t.date DESC, t.id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Long> findTransactionIdsWithCursor(
            @Param("userId") Long userId,
            @Param("accountId") Long accountId,
            @Param("categoryId") Long categoryId,
            @Param("type") String type,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("cursorDate") LocalDate cursorDate,
            @Param("cursorId") Long cursorId,
            @Param("limit") Integer limit
    );

    @EntityGraph(attributePaths = {"category", "receipts"})
    @Query("""
             SELECT t FROM Transaction t
             WHERE t.id IN :ids
             ORDER BY t.date DESC, t.id DESC
            """)
    List<Transaction> findAllByIdInOrderByDateDescIdDesc(@Param("ids") List<Long> ids);


    @Query("""
            SELECT COUNT(t) > 0 FROM Transaction t
                        WHERE t.account.user.id = :userId AND t.deletedAt IS NULL
            """)
    boolean existsByUserIdAndDeletedAtIsNull(@Param("userId") Long userId);
}
