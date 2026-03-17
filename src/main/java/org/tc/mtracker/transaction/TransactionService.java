package org.tc.mtracker.transaction;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.tc.mtracker.category.CategoryRepository;
import org.tc.mtracker.transaction.dto.TransactionCreateRequestDTO;
import org.tc.mtracker.transaction.dto.TransactionMapper;
import org.tc.mtracker.transaction.dto.TransactionResponseDTO;
import org.tc.mtracker.user.User;
import org.tc.mtracker.user.UserService;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final UserService userService;
    private final CategoryRepository categoryRepository;
    private final TransactionMapper transactionMapper;

    public TransactionResponseDTO saveTransaction(Authentication auth, TransactionCreateRequestDTO createRequestDTO) {
        User user = userService.getCurrentAuthenticatedUser(auth);


        Transaction transaction = transactionMapper.toEntity(createRequestDTO, user);
        transaction.setUser(user);

        Transaction saved = transactionRepository.save(transaction);
        return transactionMapper.toDto(saved);
    }

}
