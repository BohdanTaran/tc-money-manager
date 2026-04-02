package org.tc.mtracker.transaction;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.tc.mtracker.common.receipt.ValidReceiptFile;
import org.tc.mtracker.transaction.dto.TransactionCreateRequestDTO;
import org.tc.mtracker.transaction.dto.TransactionResponseDTO;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;

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
