package org.tc.mtracker.transaction;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.tc.mtracker.account.Account;
import org.tc.mtracker.category.Category;
import org.tc.mtracker.common.enums.TransactionType;
import org.tc.mtracker.common.file.ObjectStorageKeys;
import org.tc.mtracker.transaction.dto.TransactionCreateRequestDTO;
import org.tc.mtracker.transaction.dto.TransactionMapper;
import org.tc.mtracker.transaction.dto.TransactionResponseDTO;
import org.tc.mtracker.utils.S3Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionMutationService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final S3Service s3Service;

    public Transaction persistTransaction(Transaction transaction) {
        Transaction saved = transactionRepository.save(transaction);
        applyBalanceDelta(saved.getAccount(), saved);
        return saved;
    }

    public void addReceiptsToTransaction(List<MultipartFile> receipts, Transaction transaction) {
        if (receipts != null && !receipts.isEmpty()) {
            for (MultipartFile receipt : receipts) {
                ReceiptImage receiptImage = new ReceiptImage(UUID.randomUUID(), transaction);
                s3Service.saveFile(receiptObjectKey(receiptImage), receipt);
                transaction.addReceipt(receiptImage);
            }
        }
    }

    public void updateTransactionValues(
            Transaction transaction,
            TransactionCreateRequestDTO updateRequestDTO,
            Account targetAccount,
            Category category
    ) {
        Account currentAccount = transaction.getAccount();

        revertBalanceDelta(currentAccount, transaction);
        transactionMapper.updateEntity(updateRequestDTO, transaction);
        transaction.setAccount(targetAccount);
        transaction.setCategory(category);
        applyBalanceDelta(targetAccount, transaction);
    }

    public void deleteSingleTransaction(Transaction transaction) {
        revertBalanceDelta(transaction.getAccount(), transaction);
        deleteReceipts(transaction);
        transactionRepository.delete(transaction);
    }

    public TransactionResponseDTO toResponseDto(Transaction transaction) {
        return transactionMapper.toDto(transaction, generatePresignedUrlsForReceipts(transaction));
    }

    private void deleteReceipts(Transaction transaction) {
        transaction.getReceipts()
                .forEach(receipt -> s3Service.deleteFile(receiptObjectKey(receipt)));
    }

    private List<String> generatePresignedUrlsForReceipts(Transaction saved) {
        List<ReceiptImage> receipts = saved.getReceipts();
        if (receipts.isEmpty()) {
            return List.of();
        }
        return receipts.stream()
                .map(i -> s3Service.generatePresignedUrl(receiptObjectKey(i))).toList();
    }

    private String receiptObjectKey(ReceiptImage receiptImage) {
        return ObjectStorageKeys.receiptKey(receiptImage.getId());
    }

    private static void applyBalanceDelta(Account account, Transaction transaction) {
        account.setBalance(currentBalance(account).add(calculateDelta(transaction.getType(), transaction.getAmount())));
    }

    private static void revertBalanceDelta(Account account, Transaction transaction) {
        account.setBalance(currentBalance(account).subtract(calculateDelta(transaction.getType(), transaction.getAmount())));
    }

    private static BigDecimal calculateDelta(TransactionType type, BigDecimal amount) {
        return type == TransactionType.INCOME ? amount : amount.negate();
    }

    private static BigDecimal currentBalance(Account account) {
        return account.getBalance() == null ? BigDecimal.ZERO : account.getBalance();
    }
}
