package org.tc.mtracker.transaction.recurring;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.tc.mtracker.user.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, Long> {

    @Query("""
            SELECT rt FROM RecurringTransaction rt
            WHERE rt.nextExecutionDate <= :executionDate
            ORDER BY rt.nextExecutionDate ASC, rt.id ASC
            """)
    List<RecurringTransaction> findDueTransactions(@Param("executionDate") LocalDate executionDate);

    @Query("""
            SELECT rt FROM RecurringTransaction rt
            WHERE rt.id = :id
            AND rt.user = :user
            """)
    Optional<RecurringTransaction> findByIdAndUser(@Param("id") Long id, @Param("user") User user);

    @Query("""
            SELECT rt FROM RecurringTransaction rt
            WHERE rt.user = :user
            ORDER BY rt.nextExecutionDate ASC, rt.createdAt ASC, rt.id ASC
            """)
    List<RecurringTransaction> findAllByUserOrderBySchedule(@Param("user") User user);
}
