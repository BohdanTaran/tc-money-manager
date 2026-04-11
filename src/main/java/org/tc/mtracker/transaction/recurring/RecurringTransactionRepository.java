package org.tc.mtracker.transaction.recurring;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, Long> {

    @Query("""
            SELECT rt FROM RecurringTransaction rt
            WHERE rt.nextExecutionDate <= :executionDate
            ORDER BY rt.nextExecutionDate ASC, rt.id ASC
            """)
    List<RecurringTransaction> findDueTransactions(@Param("executionDate") LocalDate executionDate);
}
