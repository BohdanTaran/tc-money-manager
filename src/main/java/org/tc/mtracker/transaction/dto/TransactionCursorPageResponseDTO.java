package org.tc.mtracker.transaction.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Paginated response with cursor for next page")
public record TransactionCursorPageResponseDTO<T>(

        @Schema(
                description = "List of transactions on current page",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        List<T> data,

        @Schema(
                description = """
            Cursor for fetching the next page.
            - Send this value as `cursor` parameter in the next request
            - `null` indicates this is the last page
            """,
                example = "MjAyNS0wNS0yN3wxMjM0NQ==",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String nextCursor,

        @Schema(
                description = "Whether there are more transactions available after this page",
                example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Boolean hasNext,

        @Schema(
                description = "Number of transactions returned on this page (equals `limit` except possibly for last page)",
                example = "15",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Integer pageSize

) {}