package org.tc.mtracker.transaction.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.tc.mtracker.security.CustomUserDetails;
import org.tc.mtracker.transaction.TransactionService;
import org.tc.mtracker.transaction.dto.*;
import org.tc.mtracker.transaction.recurring.enums.RecurringTransactionChangeScope;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TransactionController implements TransactionApi {
    private final TransactionService transactionService;

    @Override
    public ResponseEntity<TransactionCursorPageResponseDTO<TransactionResponseDTO>> getTransactions(
            @Valid @ModelAttribute TransactionCursorRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ResponseEntity.ok(transactionService.getTransactions(request, user.getId()));
    }

    @Override
    public ResponseEntity<TransactionResponseDTO> getTransactionById(
            Long id,
            Authentication auth
    ) {
        return ResponseEntity.ok(transactionService.getTransactionById(id, auth));
    }

    @Override
    public ResponseEntity<TransactionResponseDTO> createTransaction(
            Authentication auth,
            TransactionCreateRequestDTO createRequestDTO,
            List<MultipartFile> receipts) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.createTransaction(auth, createRequestDTO, receipts));
    }

    @Override
    public ResponseEntity<TransactionResponseDTO> updateTransaction(
            Long id,
            TransactionUpdateRequestDTO updateRequestDTO,
            Authentication auth
    ) {
        return ResponseEntity.ok(transactionService.updateTransaction(id, auth, updateRequestDTO));
    }

    @Override
    public ResponseEntity<Void> deleteTransaction(
            Long id,
            RecurringTransactionChangeScope recurringScope,
            Authentication auth
    ) {
        transactionService.deleteTransaction(id, auth, recurringScope);
        return ResponseEntity.noContent().build();
    }
}
