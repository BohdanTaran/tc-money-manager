package org.tc.mtracker.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.tc.mtracker.account.Account;
import org.tc.mtracker.category.Category;
import org.tc.mtracker.common.enums.TransactionType;
import org.tc.mtracker.common.file.ObjectStorageKeys;
import org.tc.mtracker.transaction.dto.TransactionCreateRequestDTO;
import org.tc.mtracker.transaction.dto.TransactionMapper;
import org.tc.mtracker.transaction.dto.TransactionResponseDTO;
import org.tc.mtracker.user.User;
import org.tc.mtracker.user.UserService;
import org.tc.mtracker.utils.S3Service;
import org.tc.mtracker.utils.exceptions.TransactionNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final UserService userService;
    private final S3Service s3Service;
    private final TransactionValidationService transactionValidationService;

    @Transactional
    public TransactionResponseDTO createTransaction(Authentication auth, TransactionCreateRequestDTO createRequestDTO, List<MultipartFile> receipts) {
        User user = userService.getCurrentAuthenticatedUser(auth);
        transactionValidationService.validateOneTimeTransactionDate(createRequestDTO.date(), user);
        Account account = transactionValidationService.resolveAccount(user, createRequestDTO.accountId());
        Transaction transaction = transactionMapper.toEntity(createRequestDTO, user);
        Category category = transactionValidationService.resolveActiveCategory(createRequestDTO.categoryId(), user);
        transactionValidationService.validateTransactionType(createRequestDTO.type(), category, user);

        transaction.setUser(user);
        transaction.setAccount(account);
        transaction.setCategory(category);
        addReceiptsToTransaction(receipts, transaction);

        Transaction saved = persistTransaction(transaction);
        log.info("Transaction created userId={} transactionId={} accountId={} amount={} type={}",
                user.getId(), saved.getId(), account.getId(), saved.getAmount(), saved.getType());

        return toResponseDto(saved);
    }

    @Transactional
    public Transaction createAutomatedTransaction(Transaction transaction) {
        Transaction saved = persistTransaction(transaction);
        log.info("Automated transaction created userId={} transactionId={} accountId={} amount={} type={} date={}",
                saved.getUser().getId(),
                saved.getId(),
                saved.getAccount().getId(),
                saved.getAmount(),
                saved.getType(),
                saved.getDate());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<TransactionResponseDTO> getTransactions(
            Authentication auth,
            Long accountId,
            Long categoryId,
            TransactionType type,
            LocalDate dateFrom,
            LocalDate dateTo
    ) {
        User user = userService.getCurrentAuthenticatedUser(auth);
        log.debug("Loading transactions for userId={} accountId={} categoryId={} type={} dateFrom={} dateTo={}",
                user.getId(), accountId, categoryId, type, dateFrom, dateTo);

        if (accountId != null) {
            transactionValidationService.resolveAccount(user, accountId);
        }
        if (categoryId != null) {
            transactionValidationService.resolveAccessibleCategory(categoryId, user);
        }

        return transactionRepository.findAllByUserAndFilters(user, accountId, categoryId, type, dateFrom, dateTo)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public TransactionResponseDTO getTransactionById(Long transactionId, Authentication auth) {
        User user = userService.getCurrentAuthenticatedUser(auth);
        Transaction transaction = findActiveOwnedTransaction(transactionId, user);
        log.debug("Transaction returned for userId={} transactionId={}", user.getId(), transactionId);
        return toResponseDto(transaction);
    }

    @Transactional
    public TransactionResponseDTO updateTransaction(Long transactionId, Authentication auth, TransactionCreateRequestDTO updateRequestDTO) {
        User user = userService.getCurrentAuthenticatedUser(auth);
        transactionValidationService.validateOneTimeTransactionDate(updateRequestDTO.date(), user);
        Transaction transaction = findActiveOwnedTransaction(transactionId, user);

        Account currentAccount = transaction.getAccount();
        Account targetAccount = transactionValidationService.resolveAccount(user, updateRequestDTO.accountId());
        Category category = transactionValidationService.resolveActiveCategory(updateRequestDTO.categoryId(), user);

        transactionValidationService.validateTransactionType(updateRequestDTO.type(), category, user);

        revertBalanceDelta(currentAccount, transaction);
        transactionMapper.updateEntity(updateRequestDTO, transaction);
        transaction.setAccount(targetAccount);
        transaction.setCategory(category);
        applyBalanceDelta(targetAccount, transaction);

        Transaction saved = transactionRepository.save(transaction);
        log.info("Transaction updated userId={} transactionId={} accountId={} amount={} type={}",
                user.getId(), saved.getId(), targetAccount.getId(), saved.getAmount(), saved.getType());
        return toResponseDto(saved);
    }

    @Transactional
    public void deleteTransaction(Long transactionId, Authentication auth) {
        User user = userService.getCurrentAuthenticatedUser(auth);
        Transaction transaction = findActiveOwnedTransaction(transactionId, user);

        revertBalanceDelta(transaction.getAccount(), transaction);
        deleteReceipts(transaction);
        transactionRepository.delete(transaction);
        log.info("Transaction deleted userId={} transactionId={}", user.getId(), transactionId);
    }

    private static void applyBalanceDelta(Account account, Transaction transaction) {
        account.setBalance(currentBalance(account).add(calculateDelta(transaction.getType(), transaction.getAmount())));
    }

    private static void revertBalanceDelta(Account account, Transaction transaction) {
        account.setBalance(currentBalance(account).subtract(calculateDelta(transaction.getType(), transaction.getAmount())));
    }

    private void addReceiptsToTransaction(List<MultipartFile> receipts, Transaction transaction) {
        if (receipts != null && !receipts.isEmpty()) {
            log.debug("Uploading {} receipt(s) for transaction userId={}", receipts.size(), transaction.getUser().getId());
            for (MultipartFile receipt : receipts) {
                ReceiptImage receiptImage = new ReceiptImage(UUID.randomUUID(), transaction);
                s3Service.saveFile(receiptObjectKey(receiptImage), receipt);
                transaction.addReceipt(receiptImage);
            }
        }
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

    private Transaction persistTransaction(Transaction transaction) {
        Transaction saved = transactionRepository.save(transaction);
        applyBalanceDelta(saved.getAccount(), saved);
        return saved;
    }

    private TransactionResponseDTO toResponseDto(Transaction transaction) {
        return transactionMapper.toDto(transaction, generatePresignedUrlsForReceipts(transaction));
    }

    private Transaction findActiveOwnedTransaction(Long transactionId, User user) {
        return transactionRepository.findActiveByIdAndUser(transactionId, user)
                .orElseThrow(() -> {
                    log.warn("Transaction not found userId={} transactionId={}", user.getId(), transactionId);
                    return new TransactionNotFoundException("Transaction with id %d not found".formatted(transactionId));
                });
    }

    private static BigDecimal calculateDelta(TransactionType type, BigDecimal amount) {
        return type == TransactionType.INCOME ? amount : amount.negate();
    }

    private static BigDecimal currentBalance(Account account) {
        return account.getBalance() == null ? BigDecimal.ZERO : account.getBalance();
    }

}
