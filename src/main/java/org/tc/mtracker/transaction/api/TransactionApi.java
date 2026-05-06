package org.tc.mtracker.transaction.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.tc.mtracker.common.enums.TransactionType;
import org.tc.mtracker.common.receipt.ValidReceiptFile;
import org.tc.mtracker.transaction.dto.TransactionCreateRequestDTO;
import org.tc.mtracker.transaction.dto.TransactionResponseDTO;
import org.tc.mtracker.transaction.recurring.enums.RecurringTransactionChangeScope;

import java.time.LocalDate;
import java.util.List;

@RequestMapping("/api/v1/transactions")
@Tag(name = "Transaction Management", description = "Transaction management endpoints")
public interface TransactionApi {

    @Operation(
            summary = "Get transactions",
            description = "Returns the authenticated user's transactions filtered by account, category, type, and date range. Category filters can reference both active and archived categories that still exist in historical transactions."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Transactions returned",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = TransactionResponseDTO.class)))
    )
    @GetMapping
    ResponseEntity<List<TransactionResponseDTO>> getTransactions(
            @RequestParam(name = "accountId", required = false) Long accountId,
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "type", required = false) TransactionType type,
            @RequestParam(name = "dateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(name = "dateTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @Parameter(hidden = true) Authentication auth
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
    @GetMapping("/{transactionId}")
    ResponseEntity<TransactionResponseDTO> getTransactionById(
            @PathVariable Long transactionId,
            @Parameter(hidden = true) Authentication auth
    );

    @Operation(
            summary = "Create transaction",
            description = "Creates a one-time transaction for the authenticated user and optionally uploads receipt files. One-time transaction date can be in the past or today, but not in the future."
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
                    required = false,
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
            description = "Updates a transaction and recalculates related balances. For transactions generated by a recurring rule, recurringScope=ONLY_THIS updates only the selected occurrence, while recurringScope=THIS_AND_FUTURE also updates the recurring rule and already-created future occurrences. Transaction date can be in the past or today, but not in the future."
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
    @PutMapping("/{transactionId}")
    ResponseEntity<TransactionResponseDTO> updateTransaction(
            @PathVariable Long transactionId,
            @Parameter(description = "How to apply changes when the transaction belongs to a recurring rule.")
            @RequestParam(name = "recurringScope", defaultValue = "ONLY_THIS")
            RecurringTransactionChangeScope recurringScope,
            @Valid @RequestBody TransactionCreateRequestDTO updateRequestDTO,
            @Parameter(hidden = true) Authentication auth
    );

    @Operation(
            summary = "Delete transaction",
            description = "Deletes a transaction and rolls back the account balance impact. For transactions generated by a recurring rule, recurringScope=ONLY_THIS deletes only the selected occurrence, while recurringScope=THIS_AND_FUTURE deletes the selected occurrence, already-created future occurrences, and the recurring rule."
    )
    @ApiResponse(responseCode = "204", description = "Transaction deleted successfully")
    @DeleteMapping("/{transactionId}")
    ResponseEntity<Void> deleteTransaction(
            @PathVariable Long transactionId,
            @Parameter(description = "How to apply deletion when the transaction belongs to a recurring rule.")
            @RequestParam(name = "recurringScope", defaultValue = "ONLY_THIS")
            RecurringTransactionChangeScope recurringScope,
            @Parameter(hidden = true) Authentication auth
    );
}
