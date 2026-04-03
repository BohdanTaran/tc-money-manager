package org.tc.mtracker.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.tc.mtracker.common.enums.TransactionType;
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
}
