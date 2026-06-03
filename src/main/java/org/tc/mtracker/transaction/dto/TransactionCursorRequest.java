package org.tc.mtracker.transaction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.tc.mtracker.common.enums.TransactionType;

import java.time.LocalDate;

@Schema(description = "Request parameters for cursor-paginated transactions")
public record TransactionCursorRequest(

        @Schema(
                description = "Filter by account ID. Only transactions from this account will be returned.",
                example = "42"
        )
        Long accountId,

        @Schema(
                description = "Filter by category ID. Includes both active and archived categories.",
                example = "15"
        )
        Long categoryId,

        @Schema(
                description = "Filter by transaction type (INCOME or EXPENSE)",
                example = "EXPENSE"
        )
        TransactionType type,

        @Schema(
                description = "Start date for filtering (inclusive). Format: YYYY-MM-DD",
                example = "2025-01-01"
        )
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateFrom,

        @Schema(
                description = "End date for filtering (inclusive). Format: YYYY-MM-DD",
                example = "2025-12-31"
        )
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateTo,

        @Schema(
                description = """
            Opaque cursor for pagination.
            - Omit this parameter for the first page
            - Use the `nextCursor` value from the previous response for subsequent pages
            - Cursors are URL-safe base64 encoded strings
            """,
                example = "MjAyNS0wNS0yN3wxMjM0NQ=="
        )
        String cursor,

        @Schema(
                description = "Number of transactions per page. Min 1, Max 100. Default 15.",
                example = "15",
                defaultValue = "15",
                minimum = "1",
                maximum = "100"
        )
        @Min(1)
        @Max(100)
        Integer limit

) {
    public TransactionCursorRequest {
        if (limit == null) {
            limit = 15;
        }
    }

    @AssertTrue(message = "dateFrom must be before or equal to dateTo")
    public boolean isDateRangeValid() {
        return dateFrom == null || dateTo == null || !dateFrom.isAfter(dateTo);
    }
}
