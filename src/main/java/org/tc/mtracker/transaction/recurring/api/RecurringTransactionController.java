package org.tc.mtracker.transaction.recurring.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;
import org.tc.mtracker.transaction.recurring.RecurringTransactionService;
import org.tc.mtracker.transaction.recurring.dto.RecurringTransactionCreateRequestDTO;
import org.tc.mtracker.transaction.recurring.dto.RecurringTransactionResponseDTO;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RecurringTransactionController implements RecurringTransactionApi {

    private final RecurringTransactionService recurringTransactionService;

    @Override
    public ResponseEntity<List<RecurringTransactionResponseDTO>> getRecurringTransactions(Authentication auth) {
        return ResponseEntity.ok(recurringTransactionService.getRecurringTransactions(auth));
    }

    @Override
    public ResponseEntity<RecurringTransactionResponseDTO> getRecurringTransactionById(Long recurringTransactionId, Authentication auth) {
        return ResponseEntity.ok(recurringTransactionService.getRecurringTransactionById(recurringTransactionId, auth));
    }

    @Override
    public ResponseEntity<RecurringTransactionResponseDTO> createRecurringTransaction(
            RecurringTransactionCreateRequestDTO requestDTO,
            Authentication auth
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recurringTransactionService.createRecurringTransaction(auth, requestDTO));
    }

    @Override
    public ResponseEntity<Void> deleteRecurringTransaction(Long recurringTransactionId, Authentication auth) {
        recurringTransactionService.deleteRecurringTransaction(recurringTransactionId, auth);
        return ResponseEntity.noContent().build();
    }
}
