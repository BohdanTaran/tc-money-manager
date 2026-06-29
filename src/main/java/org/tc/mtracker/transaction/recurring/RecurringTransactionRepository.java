package org.tc.mtracker.transaction.recurring;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.tc.mtracker.category.Category;
import org.tc.mtracker.category.enums.CategoryStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, Long> {

    @Query("""
            SELECT rt FROM RecurringTransaction rt
            WHERE rt.nextExecutionDate <= :executionDate
            AND rt.category.status = :status
            ORDER BY rt.nextExecutionDate ASC, rt.id ASC
            """)
    List<RecurringTransaction> findDueTransactions(
            @Param("executionDate") LocalDate executionDate,
            @Param("status") CategoryStatus status
    );

    @Query("""
            SELECT rt FROM RecurringTransaction rt
            WHERE rt.id = :id
            AND rt.account.user.id = :userId
            """)
    Optional<RecurringTransaction> findByIdAndUserId(
            @Param("id") Long id,
            @Param("userId") Long userId);

    @Query("""
            SELECT rt FROM RecurringTransaction rt
            WHERE rt.account.user.id = :userId
            ORDER BY rt.nextExecutionDate ASC, rt.createdAt ASC, rt.id ASC
            """)
    List<RecurringTransaction> findAllByUserOrderBySchedule(@Param("userId") Long userId);

    @Query("""
            SELECT COUNT(rt) FROM RecurringTransaction rt
            WHERE rt.account.user.id = :userId AND rt.category = :category
            """)
    long countByUserAndCategory(
            @Param("userId") Long userId,
            @Param("category") Category category);

    @Query("""
            SELECT COUNT(rt) > 0 FROM RecurringTransaction rt
            WHERE rt.account.user.id = :userId
            """)
    boolean existsByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("""
            UPDATE RecurringTransaction rt
            SET rt.category = :replacementCategory
            WHERE rt.account.user.id = :userId
            AND rt.category = :sourceCategory
            """)
    void reassignCategory(
            @Param("userId") Long userId,
            @Param("sourceCategory") Category sourceCategory,
            @Param("replacementCategory") Category replacementCategory
    );
}
