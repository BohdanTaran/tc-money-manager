package org.tc.mtracker.transaction.recurring.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.tc.mtracker.transaction.recurring.dto.RecurringTransactionCreateRequestDTO;
import org.tc.mtracker.transaction.recurring.dto.RecurringTransactionResponseDTO;

import java.util.List;

@RequestMapping("/api/v1/recurring-transactions")
@Tag(name = "Recurring Transaction Management", description = "Recurring transaction management endpoints")
public interface RecurringTransactionApi {

    @Operation(
            summary = "Get recurring transactions",
            description = "Returns the authenticated user's recurring transactions ordered by next execution date."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Recurring transactions returned",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = RecurringTransactionResponseDTO.class))
            )
    )
    @GetMapping
    ResponseEntity<List<RecurringTransactionResponseDTO>> getRecurringTransactions(
            @Parameter(hidden = true) Authentication auth
    );

    @Operation(
            summary = "Get recurring transaction by id",
            description = "Returns a recurring transaction owned by the authenticated user."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Recurring transaction returned",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = RecurringTransactionResponseDTO.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "Recurring transaction not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @GetMapping("/{recurringTransactionId}")
    ResponseEntity<RecurringTransactionResponseDTO> getRecurringTransactionById(
            @PathVariable Long recurringTransactionId,
            @Parameter(hidden = true) Authentication auth
    );

    @Operation(
            summary = "Create recurring transaction",
            description = "Creates a recurring transaction. Start date can be today or in the future. If the start date is today, the first transaction is created immediately. If the start date is in the future, the first transaction is scheduled for that date."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Recurring transaction created",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = RecurringTransactionResponseDTO.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid input data or validation failed",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @PostMapping
    ResponseEntity<RecurringTransactionResponseDTO> createRecurringTransaction(
            @Valid @RequestBody RecurringTransactionCreateRequestDTO requestDTO,
            @Parameter(hidden = true) Authentication auth
    );

    @Operation(
            summary = "Delete recurring transaction",
            description = "Deletes a recurring transaction. Future executions will no longer be created."
    )
    @ApiResponse(responseCode = "204", description = "Recurring transaction deleted successfully")
    @ApiResponse(
            responseCode = "404",
            description = "Recurring transaction not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @DeleteMapping("/{recurringTransactionId}")
    ResponseEntity<Void> deleteRecurringTransaction(
            @PathVariable Long recurringTransactionId,
            @Parameter(hidden = true) Authentication auth
    );
}
