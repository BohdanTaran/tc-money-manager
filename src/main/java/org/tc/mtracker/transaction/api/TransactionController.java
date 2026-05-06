package org.tc.mtracker.transaction.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.tc.mtracker.common.enums.TransactionType;
import org.tc.mtracker.transaction.TransactionService;
import org.tc.mtracker.transaction.dto.TransactionCreateRequestDTO;
import org.tc.mtracker.transaction.dto.TransactionResponseDTO;
import org.tc.mtracker.transaction.recurring.enums.RecurringTransactionChangeScope;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class TransactionController implements TransactionApi {
    private final TransactionService transactionService;

    @Override
    public ResponseEntity<List<TransactionResponseDTO>> getTransactions(
            Long accountId,
            Long categoryId,
            TransactionType type,
            LocalDate dateFrom,
            LocalDate dateTo,
            Authentication auth
    ) {
        return ResponseEntity.ok(transactionService.getTransactions(auth, accountId, categoryId, type, dateFrom, dateTo));
    }

    @Override
    public ResponseEntity<TransactionResponseDTO> getTransactionById(
            Long transactionId,
            Authentication auth
    ) {
        return ResponseEntity.ok(transactionService.getTransactionById(transactionId, auth));
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
            Long transactionId,
            RecurringTransactionChangeScope recurringScope,
            TransactionCreateRequestDTO updateRequestDTO,
            Authentication auth
    ) {
        return ResponseEntity.ok(transactionService.updateTransaction(transactionId, auth, updateRequestDTO, recurringScope));
    }

    @Override
    public ResponseEntity<Void> deleteTransaction(
            Long transactionId,
            RecurringTransactionChangeScope recurringScope,
            Authentication auth
    ) {
        transactionService.deleteTransaction(transactionId, auth, recurringScope);
        return ResponseEntity.noContent().build();
    }
}
