package org.tc.mtracker.transaction;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.tc.mtracker.category.Category;
import org.tc.mtracker.category.CategoryService;
import org.tc.mtracker.transaction.dto.TransactionCreateRequestDTO;
import org.tc.mtracker.transaction.dto.TransactionMapper;
import org.tc.mtracker.transaction.dto.TransactionResponseDTO;
import org.tc.mtracker.user.User;
import org.tc.mtracker.user.UserService;
import org.tc.mtracker.utils.S3Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final CategoryService categoryService;
    private final TransactionMapper transactionMapper;
    private final UserService userService;
    private final S3Service s3Service;

    @Transactional
    public TransactionResponseDTO saveTransaction(Authentication auth, TransactionCreateRequestDTO createRequestDTO, List<MultipartFile> receipts) {
        User user = userService.getCurrentAuthenticatedUser(auth);

        Transaction transaction = transactionMapper.toEntity(createRequestDTO, user);
        Category category = categoryService.findById(createRequestDTO.categoryId());

        validateTransactionType(createRequestDTO, category);

        transaction.setUser(user);
        transaction.setCategory(category);
        addReceiptsToTransaction(receipts, transaction);

        Transaction saved = transactionRepository.save(transaction);

        List<String> presignedUrls = generatePresignedUrlsForReceipts(saved);

        return transactionMapper.toDto(saved, presignedUrls);
    }

    private void addReceiptsToTransaction(List<MultipartFile> receipts, Transaction transaction) {
        if (receipts != null && !receipts.isEmpty()) {
            for (MultipartFile receipt : receipts) {
                ReceiptImage receiptImage = new ReceiptImage(UUID.randomUUID(), transaction);
                s3Service.saveFile(receiptImage.getId().toString(), receipt);
                transaction.addReceipt(receiptImage);
            }
        }
    }

    private List<String> generatePresignedUrlsForReceipts(Transaction saved) {

        List<ReceiptImage> receipts = saved.getReceipts();
        if (receipts.isEmpty()) {
            return List.of();
        }
        return receipts.stream()
                .map(i -> s3Service.generatePresignedUrl(i.getId().toString())).toList();
    }

    private static void validateTransactionType(TransactionCreateRequestDTO createRequestDTO, Category category) {
        if (!category.getType().equals(createRequestDTO.type())) {
            throw new IllegalArgumentException("Category type does not match transaction type");
        }
    }

}
