package org.tc.mtracker.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.tc.mtracker.user.User;

import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    @Query("""
                SELECT t FROM Transaction t
                WHERE t.id = :id
                AND t.user = :user
                AND t.deletedAt IS NULL
            """)
    Optional<Transaction> findActiveByIdAndUser(@Param("id") Long id, @Param("user") User user);
}
