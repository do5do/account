package com.zerobase.account.service;

import com.zerobase.account.domain.Account;
import com.zerobase.account.domain.AccountUser;
import com.zerobase.account.domain.Transaction;
import com.zerobase.account.dto.TransactionDto;
import com.zerobase.account.exception.AccountException;
import com.zerobase.account.repository.AccountRepository;
import com.zerobase.account.repository.AccountUserRepository;
import com.zerobase.account.repository.TransactionRepository;
import com.zerobase.account.type.TransactionResultType;
import com.zerobase.account.type.TransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import static com.zerobase.account.type.AccountStatus.IN_USE;
import static com.zerobase.account.type.ErrorCode.*;
import static com.zerobase.account.type.TransactionResultType.F;
import static com.zerobase.account.type.TransactionResultType.S;
import static com.zerobase.account.type.TransactionType.CANCEL;
import static com.zerobase.account.type.TransactionType.USE;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountUserRepository accountUserRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public TransactionDto useBalance(Long userId, String accountNumber, Long amount) {
        AccountUser accountUser = accountUserRepository.findById(userId)
                .orElseThrow(() -> new AccountException(USER_NOT_FOUND));

        Account account = getAccount(accountNumber);

        validUseBalance(accountUser, account, amount);

        account.useBalance(amount);

        return TransactionDto.fromEntity(saveAndGetTransaction(USE, S, amount, account));
    }

    private void validUseBalance(AccountUser accountUser, Account account, Long amount) {
        if (!Objects.equals(accountUser.getId(), account.getAccountUser().getId())) {
            throw new AccountException(USER_ACCOUNT_UN_MATCH);
        }

        if (account.getAccountStatus() != IN_USE) {
            throw new AccountException(ACCOUNT_ALREADY_UNREGISTERED);
        }

        if (account.getBalance() < amount) {
            throw new AccountException(AMOUNT_EXCEED_BALANCE);
        }
    }

    @Transactional
    public void saveFailedUseTransaction(String accountNumber, Long amount) {
        Account account = getAccount(accountNumber);
        saveAndGetTransaction(USE, F, amount, account);
    }

    private Transaction saveAndGetTransaction(TransactionType transactionType,
                                              TransactionResultType transactionResultType,
                                              Long amount,
                                              Account account) {
        return transactionRepository.save(Transaction.builder()
                .transactionType(transactionType)
                .transactionResultType(transactionResultType)
                .account(account)
                .amount(amount)
                .balanceSnapshot(account.getBalance())
                // UUID는 중간에 데쉬가 두개 들어가는데 이를 제거하여 많이 사용함
                .transactionId(UUID.randomUUID().toString().replace("-", ""))
                .transactedAt(LocalDateTime.now())
                .build());
    }

    @Transactional
    public TransactionDto cancelBalance(String transactionId, String accountNumber, Long amount) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new AccountException(TRANSACTION_NOT_FOUND));

        Account account = getAccount(accountNumber);

        validateCancelBalance(transaction, account, amount);

        account.cancelBalance(amount);

        return TransactionDto.fromEntity(saveAndGetTransaction(CANCEL, S, amount, account));
    }

    private void validateCancelBalance(Transaction transaction, Account account, Long amount) {
        if (!Objects.equals(transaction.getAccount().getId(), account.getId())) {
            throw new AccountException(TRANSACTION_ACCOUNT_UN_MATCH);
        }

        if (!Objects.equals(transaction.getAmount(), amount)) { // 거래 금액과 거래 취소 금액이 다른 경우
            throw new AccountException(CANCEL_MUST_FULLY);
        }

        if (transaction.getTransactedAt().isBefore(LocalDateTime.now().minusYears(1))) {
            throw new AccountException(TOO_OLD_ORDER_TO_CANCEL);
        }
    }

    @Transactional
    public void saveFailedCancelTransaction(String accountNumber, Long amount) {
        Account account = getAccount(accountNumber);
        saveAndGetTransaction(CANCEL, F, amount, account);
    }

    public TransactionDto queryTransaction(String transactionId) {
        return TransactionDto.fromEntity(
                transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new AccountException(TRANSACTION_NOT_FOUND)));
    }

    private Account getAccount(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));
    }
}
