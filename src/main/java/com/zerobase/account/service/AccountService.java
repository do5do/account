package com.zerobase.account.service;

import com.zerobase.account.domain.Account;
import com.zerobase.account.domain.AccountUser;
import com.zerobase.account.dto.AccountDto;
import com.zerobase.account.exception.AccountException;
import com.zerobase.account.repository.AccountRepository;
import com.zerobase.account.repository.AccountUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static com.zerobase.account.type.AccountStatus.IN_USE;
import static com.zerobase.account.type.AccountStatus.UNREGISTERED;
import static com.zerobase.account.type.ErrorCode.*;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {
    private final AccountRepository accountRepository;
    private final AccountUserRepository accountUserRepository;
    private static final String DEFAULT_ACCOUNT_NUMBER = "1000000000";
    private static final int MAX_ACCOUNT_SIZE = 10;

    @Transactional
    public AccountDto createAccount(Long userId, Long initialBalance) {
        AccountUser accountUser = getAccountUser(userId);
        validateCreateAccount(accountUser);

        String newAccountNumber = accountRepository.findFirstByOrderByIdDesc()
                .map(account -> String.valueOf(Integer.parseInt(account.getAccountNumber()) + 1))
                .orElse(DEFAULT_ACCOUNT_NUMBER);

        validateCreateAccountNumber(newAccountNumber);

        return AccountDto.fromEntity(
                accountRepository.save(Account.builder()
                        .accountUser(accountUser)
                        .accountNumber(newAccountNumber)
                        .accountStatus(IN_USE)
                        .balance(initialBalance)
                        .registeredAt(LocalDateTime.now())
                        .build()));
    }

    private void validateCreateAccountNumber(String newAccountNumber) {
        accountRepository.findByAccountNumber(newAccountNumber)
                .ifPresent(o -> {
                    throw new AccountException(ACCOUNT_ALREADY_EXIST);
                });
    }

    private void validateCreateAccount(AccountUser accountUser) {
        if (accountRepository.countByAccountUser(accountUser) >= MAX_ACCOUNT_SIZE) {
            throw new AccountException(MAX_ACCOUNT_PER_USER_10);
        }
    }

    @Transactional
    public AccountDto deleteAccount(Long userId, String accountNumber) {
        AccountUser accountUser = getAccountUser(userId);

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));

        validateDeleteAccount(accountUser, account);

        // 상태 변경
        account.changeAccountForDelete();
        return AccountDto.fromEntity(account);
    }

    private void validateDeleteAccount(AccountUser accountUser, Account account) {
        if (!Objects.equals(accountUser.getId(), account.getAccountUser().getId())) {
            throw new AccountException(USER_ACCOUNT_UN_MATCH);
        }

        if (account.getAccountStatus() == UNREGISTERED) {
            throw new AccountException(ACCOUNT_ALREADY_UNREGISTERED);
        }

        if (account.getBalance() > 0) {
            throw new AccountException(BALANCE_NOT_EMPTY);
        }
    }

    public List<AccountDto> getAccountsByUserId(Long userId) {
        AccountUser accountUser = getAccountUser(userId);

        return accountRepository.findByAccountUser(accountUser)
                .stream().map(AccountDto::fromEntity).toList();
    }

    private AccountUser getAccountUser(Long userId) {
        return accountUserRepository.findById(userId)
                .orElseThrow(() -> new AccountException(USER_NOT_FOUND));
    }
}
