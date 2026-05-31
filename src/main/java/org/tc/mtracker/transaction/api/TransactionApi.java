package org.tc.mtracker.transaction.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.tc.mtracker.common.receipt.ValidReceiptFile;
import org.tc.mtracker.security.CustomUserDetails;
import org.tc.mtracker.transaction.dto.*;
import org.tc.mtracker.transaction.recurring.enums.RecurringTransactionChangeScope;

import java.util.List;

@RequestMapping("/api/v1/transactions")
@Tag(name = "Transaction Management", description = "Transaction management endpoints")
public interface TransactionApi {

    @Operation(
            summary = "Get paginated transactions",
            description = """
                    Returns the authenticated user's transactions filtered by account, category, type, and date range.
                    
                    **Pagination:** Uses cursor-based pagination (not page numbers) for consistent ordering even when
                    new transactions are added. The first request omits the `cursor` parameter. Subsequent requests
                    should use the `nextCursor` value returned in the response.
                    
                    **Sorting:** Always returns transactions sorted from newest to oldest by date, then by ID.
                    
                    **Filters:** Category filters can reference both active and archived categories that still exist
                    in historical transactions.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Transactions returned successfully",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = TransactionCursorPageResponseDTO.class)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid request parameters (e.g., limit out of range 1-100)"
    )
    @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - authentication required"
    )
    @GetMapping
    ResponseEntity<TransactionCursorPageResponseDTO<TransactionResponseDTO>> getTransactions(
            @Valid @ModelAttribute
            @Parameter(description = "Pagination and filter parameters")
            TransactionCursorRequest request,
            CustomUserDetails user
    );


    @Operation(
            summary = "Get transaction by id",
            description = "Returns one transaction accessible to the authenticated user."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Transaction returned",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = TransactionResponseDTO.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "Transaction not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @GetMapping("/{id}")
    ResponseEntity<TransactionResponseDTO> getTransactionById(
            @PathVariable Long id,
            @Parameter(hidden = true) Authentication auth
    );

    @Operation(
            summary = "Create transaction",
            description = "Creates a one-time transaction for the authenticated user and optionally uploads receipt " +
                    "files. One-time transaction date can be in the past or today, but not in the future."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Transaction created successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = TransactionResponseDTO.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid input data or transaction date is in the future",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<TransactionResponseDTO> createTransaction(
            @Parameter(hidden = true) Authentication auth,

            @Parameter(
                    name = "Transaction dto",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TransactionCreateRequestDTO.class)
                    )
            )
            @RequestPart(name = "dto") @Valid TransactionCreateRequestDTO createRequestDTO,

            @Parameter(
                    name = "receipts",
                    description = "Allowed formats: jpg, jpeg, png, webp, pdf. Maximum 10 files.",
                    content = {
                            @Content(mediaType = "image/jpeg", schema = @Schema(type = "string", format = "binary")),
                            @Content(mediaType = "image/png", schema = @Schema(type = "string", format = "binary")),
                            @Content(mediaType = "image/webp", schema = @Schema(type = "string", format = "binary")),
                            @Content(mediaType = "application/pdf", schema = @Schema(type = "string", format = "binary"))
                    }
            )
            @RequestPart(name = "receipts", required = false)
            @Size(max = 10)
            List<@ValidReceiptFile MultipartFile> receipts
    );

    @Operation(
            summary = "Update transaction",
            description = "Updates a transaction and recalculates related balances. For transactions generated by " +
                    "a recurring rule, recurringScope=ONLY_THIS updates only the selected occurrence, " +
                    "while recurringScope=THIS_AND_FUTURE also updates the recurring rule and " +
                    "future occurrences. Transaction date can be in the past or today, " +
                    "but not in the future."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Transaction updated successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = TransactionResponseDTO.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid input data or transaction date is in the future",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @PatchMapping("/{id}")
    ResponseEntity<TransactionResponseDTO> updateTransaction(
            @PathVariable Long id,
            @Parameter(description = "How to apply changes when the transaction belongs to a recurring rule.")
            @Valid @RequestBody TransactionUpdateRequestDTO updateRequestDTO,
            @Parameter(hidden = true) Authentication auth
    );

    @Operation(
            summary = "Delete transaction",
            description = "Deletes a transaction and rolls back the account balance impact. For transactions " +
                    "generated by a recurring rule, recurringScope=ONLY_THIS deletes only the selected occurrence," +
                    " while recurringScope=THIS_AND_FUTURE deletes the selected occurrence," +
                    " already-created future occurrences, and the recurring rule."
    )
    @ApiResponse(responseCode = "204", description = "Transaction deleted successfully")
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteTransaction(
            @PathVariable Long id,
            @Parameter(description = "How to apply deletion when the transaction belongs to a recurring rule.")
            @RequestParam(name = "recurringScope", defaultValue = "ONLY_THIS")
            RecurringTransactionChangeScope recurringScope,
            @Parameter(hidden = true) Authentication auth
    );
}
