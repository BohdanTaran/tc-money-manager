package org.tc.mtracker.transaction;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.tc.mtracker.common.enums.TransactionType;
import org.tc.mtracker.common.receipt.ValidReceiptFile;
import org.tc.mtracker.transaction.dto.TransactionCreateRequestDTO;
import org.tc.mtracker.transaction.dto.TransactionResponseDTO;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;

    @GetMapping
    public ResponseEntity<List<TransactionResponseDTO>> getTransactions(
            @RequestParam(name = "accountId", required = false) Long accountId,
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "type", required = false) TransactionType type,
            @RequestParam(name = "dateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(name = "dateTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            Authentication auth
    ) {
        return ResponseEntity.ok(transactionService.getTransactions(auth, accountId, categoryId, type, dateFrom, dateTo));
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponseDTO> getTransactionById(
            @PathVariable Long transactionId,
            Authentication auth
    ) {
        return ResponseEntity.ok(transactionService.getTransactionById(transactionId, auth));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TransactionResponseDTO> createTransaction(
            Authentication auth,
            @Parameter(
                    name = "Transaction dto",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TransactionCreateRequestDTO.class)
                    )

            )
            @RequestPart(name = "dto")
            @Valid
            TransactionCreateRequestDTO createRequestDTO,

            @Parameter(
                    name = "receipts",
                    required = false,
                    content = {
                            @Content(mediaType = "image/jpeg", schema = @Schema(type = "string", format = "binary")),
                            @Content(mediaType = "image/png", schema = @Schema(type = "string", format = "binary")),
                            @Content(mediaType = "image/webp", schema = @Schema(type = "string", format = "binary")),
                            @Content(mediaType = "application/pdf", schema = @Schema(type = "string", format = "binary"))
                    }
            )
            @RequestPart(name = "receipts", required = false)
            @Size(max = 10)
            List<@ValidReceiptFile MultipartFile> receipts) {
        TransactionResponseDTO transactionResponseDTO = transactionService.createTransaction(auth, createRequestDTO, receipts);
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionResponseDTO);
    }

    @PutMapping("/{transactionId}")
    public ResponseEntity<TransactionResponseDTO> updateTransaction(
            @PathVariable Long transactionId,
            @Valid @RequestBody TransactionCreateRequestDTO updateRequestDTO,
            Authentication auth
    ) {
        return ResponseEntity.ok(transactionService.updateTransaction(transactionId, auth, updateRequestDTO));
    }

    @DeleteMapping("/{transactionId}")
    public ResponseEntity<Void> deleteTransaction(
            @PathVariable Long transactionId,
            Authentication auth
    ) {
        transactionService.deleteTransaction(transactionId, auth);
        return ResponseEntity.noContent().build();
    }
}
