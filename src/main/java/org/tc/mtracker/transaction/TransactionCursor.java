package org.tc.mtracker.transaction;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;

@Slf4j
@Builder
@Getter
public class TransactionCursor {
    private final LocalDate date;
    private final Long id;

    public static TransactionCursor fromTransaction(Transaction transaction) {
        return TransactionCursor.builder()
                .date(transaction.getDate())
                .id(transaction.getId())
                .build();
    }

    public String encode() {
        String raw = date.toString() + "|" + id;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static TransactionCursor decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }

        try {
            String decoded = new String(
                    Base64.getUrlDecoder().decode(cursor),
                    StandardCharsets.UTF_8
            );
            String[] parts = decoded.split("\\|");
            return TransactionCursor.builder()
                    .date(LocalDate.parse(parts[0]))
                    .id(Long.parseLong(parts[1]))
                    .build();
        } catch (Exception e) {
            log.warn("Invalid cursor format: {}", cursor, e);
            return null;
        }
    }
}