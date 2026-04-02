package org.tc.mtracker.transaction.dto;

import org.tc.mtracker.category.dto.CategoryResponseDTO;
import org.tc.mtracker.common.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TransactionResponseDTO(
        Long id,
        Long accountId,
        BigDecimal amount,
        CategoryResponseDTO category,
        String description,
        TransactionType type,
        List<String> receiptsUrls,
        LocalDate date,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

}
