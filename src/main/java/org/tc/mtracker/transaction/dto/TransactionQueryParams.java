package org.tc.mtracker.transaction.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Builder
@Getter
public class TransactionQueryParams {
    private Long userId;
    private Long accountId;
    private Long categoryId;
    private String type;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private LocalDate cursorDate;
    private Long cursorId;
    private int limit;
}