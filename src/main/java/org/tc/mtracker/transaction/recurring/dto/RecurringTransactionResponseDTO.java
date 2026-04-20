package org.tc.mtracker.transaction.recurring.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.tc.mtracker.category.dto.CategoryResponseDTO;
import org.tc.mtracker.common.enums.TransactionType;
import org.tc.mtracker.transaction.recurring.enums.IntervalUnit;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "Recurring transaction")
public record RecurringTransactionResponseDTO(
        @Schema(description = "Recurring transaction ID", example = "1")
        Long id,

        @Schema(description = "Account ID", example = "1")
        Long accountId,

        @Schema(description = "Transaction amount", example = "2500.00")
        BigDecimal amount,

        @Schema(description = "Category")
        CategoryResponseDTO category,

        @Schema(description = "Optional description", example = "Monthly salary")
        String description,

        @Schema(description = "Transaction type", example = "INCOME")
        TransactionType type,

        @Schema(description = "Recurring start date", example = "2026-05-01")
        LocalDate startDate,

        @Schema(description = "Next execution date", example = "2026-06-01")
        LocalDate nextExecutionDate,

        @Schema(description = "Recurring interval", example = "MONTHLY")
        IntervalUnit intervalUnit,

        @Schema(description = "Creation timestamp")
        LocalDateTime createdAt,

        @Schema(description = "Last update timestamp")
        LocalDateTime updatedAt
) {
}
