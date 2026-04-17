package org.tc.mtracker.transaction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.tc.mtracker.category.dto.CategoryResponseDTO;
import org.tc.mtracker.common.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "One-time transaction")
public record TransactionResponseDTO(
        @Schema(description = "Transaction ID", example = "1")
        Long id,

        @Schema(description = "Account ID", example = "1")
        Long accountId,

        @Schema(description = "Transaction amount", example = "125.50")
        BigDecimal amount,

        @Schema(description = "Category")
        CategoryResponseDTO category,

        @Schema(description = "Optional description", example = "Groceries")
        String description,

        @Schema(description = "Transaction type", example = "EXPENSE")
        TransactionType type,

        @Schema(description = "Receipt URLs")
        List<String> receiptsUrls,

        @Schema(description = "Transaction date", example = "2026-04-17")
        LocalDate date,

        @Schema(description = "Creation timestamp")
        LocalDateTime createdAt,

        @Schema(description = "Last update timestamp")
        LocalDateTime updatedAt
) {

}
