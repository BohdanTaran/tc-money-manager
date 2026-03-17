package org.tc.mtracker.transaction.dto;

import org.tc.mtracker.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionCreateRequestDTO(
        BigDecimal amount,

        TransactionType type,

        Long categoryId,

        LocalDate date,

        String description
) {
}
