package org.tc.mtracker.transaction;

import org.springframework.data.jpa.repository.JpaRepository;

interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
