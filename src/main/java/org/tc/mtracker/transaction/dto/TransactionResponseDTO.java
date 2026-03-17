package org.tc.mtracker.transaction.dto;

import org.tc.mtracker.category.dto.CategoryResponseDTO;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionResponseDTO(
        BigDecimal amount,
        CategoryResponseDTO category,
        LocalDate date
) {

}
