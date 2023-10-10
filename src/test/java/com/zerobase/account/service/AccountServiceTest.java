package com.zerobase.account.service;

import com.zerobase.account.domain.Account;
import com.zerobase.account.domain.AccountUser;
import com.zerobase.account.dto.AccountDto;
import com.zerobase.account.exception.AccountException;
import com.zerobase.account.repository.AccountRepository;
import com.zerobase.account.repository.AccountUserRepository;
import com.zerobase.account.type.AccountStatus;
import com.zerobase.account.type.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // @SpringBootTest 대신 mockito 사용
class AccountServiceTest {
    @Mock // 가짜로 생성
    AccountRepository accountRepository;

    @Mock
    AccountUserRepository accountUserRepository;

    @InjectMocks // 위에서 생성한 가짜 객체를 해당 객체에 주입
    AccountService accountService;

    @Test
    @DisplayName("계좌 생성 성공")
    void successCreateAccount() {
        // given
        AccountUser accountUser = getAccountUser();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));

        given(accountRepository.findFirstByOrderByIdDesc())
                .willReturn(Optional.of(Account.builder()
                        .accountNumber("1000000012")
                        .build()));

        given(accountRepository.save(any()))
                .willReturn(Account.builder()
                        .accountUser(accountUser)
                        .accountNumber("1000000015")
                        .build());

        // 가장 마지막 계좌를 가져와서 1 증가 시켰는지 어떻게 증명할 수 있을까?
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        // when
        AccountDto accountDto =
                accountService.createAccount(21L, 1000L);

        // then
        verify(accountRepository, times(1)).save(captor.capture());

        // willReturn으로 생성한 값만 가지고 있음
        assertEquals(1L, accountDto.getUserId());
        assertEquals("1000000013", captor.getValue().getAccountNumber()); // captor로 캡쳐한 값으로 확인
    }

    @Test
    @DisplayName("첫 계좌 생성")
    void createFirstAccount() {
        // given
        AccountUser accountUser = getAccountUser();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));

        given(accountRepository.findFirstByOrderByIdDesc())
                .willReturn(Optional.empty()); // 아무것도 없을 때

        given(accountRepository.save(any()))
                .willReturn(Account.builder()
                        .accountUser(accountUser)
                        .accountNumber("1000000015")
                        .build());

        // captor
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        // when
        AccountDto accountDto =
                accountService.createAccount(15L, 1000L);

        // then
        verify(accountRepository, times(1)).save(captor.capture());

        // willReturn으로 생성한 값만 가지고 있음
        assertEquals(1L, accountDto.getUserId());
        assertEquals("1000000000", captor.getValue().getAccountNumber()); // captor로 캡쳐한 값으로 확인
    }

    @Test
    @DisplayName("해당 유저 없음 - 계좌 생성 실패")
    void createAccountUserNotFound() {
        // given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.createAccount(1L, 1000L));

        // then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("유저 당 최대 계좌는 10개 - 계좌 생성 실패")
    void createAccountMaxAccountIs10() {
        // given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(getAccountUser()));

        given(accountRepository.countByAccountUser(any()))
                .willReturn(10);

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.createAccount(1L, 1000L));

        // then
        assertEquals(ErrorCode.MAX_ACCOUNT_PER_USER_10, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 해지 성공")
    void successDeleteAccount() {
        // given
        AccountUser accountUser = getAccountUser();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(accountUser)
                        .accountNumber("1000000000")
                        .balance(0L)
                        .build()));

        // when
        AccountDto accountDto = accountService.deleteAccount(10L, "1234567890");
        Account account = accountRepository.findByAccountNumber("1000000000").get();

        // then
        assertEquals(1L, accountDto.getUserId());
        assertEquals("1000000000", accountDto.getAccountNumber());

        assertEquals(AccountStatus.UNREGISTERED, account.getAccountStatus());
        assertNotEquals(null, account.getUnRegisteredAt());
    }

    @Test
    @DisplayName("해당 유저 없음 - 계좌 해지 실패")
    void deleteAccountUserNotFound() {
        // given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1000000000"));

        // then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("해당 계좌 없음 - 계좌 해지 실패")
    void deleteAccountAccountNotFound() {
        // given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(getAccountUser()));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1000000000"));

        // then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 소유주 다름 - 계좌 해지 실패")
    void deleteAccountUserAccountUnMatch() {
        // given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(getAccountUser()));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(AccountUser.builder().id(2L).build())
                        .accountNumber("1000000000")
                        .balance(0L)
                        .build()));

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1000000000"));

        // then
        assertEquals(ErrorCode.USER_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 해지 상태 - 계좌 해지 실패")
    void deleteAccountAlreadyUnregistered() {
        // given
        AccountUser accountUser = getAccountUser();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(accountUser)
                        .accountNumber("1000000000")
                        .balance(0L)
                        .accountStatus(AccountStatus.UNREGISTERED)
                        .build()));

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1000000000"));

        // then
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 잔액 있음 - 계좌 해지 실패")
    void deleteAccountBalanceIsNotEmpty() {
        // given
        AccountUser accountUser = getAccountUser();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(accountUser)
                        .accountNumber("1000000000")
                        .balance(100L)
                        .build()));

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1000000000"));

        // then
        assertEquals(ErrorCode.BALANCE_NOT_EMPTY, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 조회")
    void successGetAccountsByUserId() {
        // given
        AccountUser accountUser = getAccountUser();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));

        given(accountRepository.findByAccountUser(accountUser))
                .willReturn(getAccounts(accountUser));

        // when
        List<AccountDto> accountDtos = accountService.getAccountsByUserId(1L);

        // then
        assertEquals(3, accountDtos.size());
    }

    @Test
    @DisplayName("해당 유저 없음 - 계좌 조회 실패")
    void failedToGetAccounts() {
        // given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.getAccountsByUserId(1L));

        // then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    private static List<Account> getAccounts(AccountUser accountUser) {
        return List.of(
                Account.builder().accountUser(accountUser).balance(10L)
                        .accountNumber("1000000000").build(),
                Account.builder().accountUser(accountUser).balance(100L)
                        .accountNumber("1000000001").build(),
                Account.builder().accountUser(accountUser).balance(1000L)
                        .accountNumber("1000000002").build()
        );
    }

    private static AccountUser getAccountUser() {
        return AccountUser.builder()
                .id(1L)
                .name("Pobi")
                .build();
    }

    @Test
    @DisplayName("테스트 에시")
    void testMockito() {
        // given
        given(accountRepository.findById(anyLong()))
                .willReturn(Optional.of(Account.builder()
                        .accountStatus(AccountStatus.UNREGISTERED)
                        .accountNumber("65789")
                        .build()));

        // 의존하고 있는 mock에 전달된 데이터가 내가 의도한 데이터가 맞는지 검증
        // ArgumentCaptor : 메소드에 전달된 인수를 캡쳐할 수 있다.
        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);

        // when
        Account account = accountRepository.findById(455L).get(); // anyLong 이기때문에 아무 숫자나 넣어도 된다.

        // then
        // 의존하고 있는 mock이 해당 동작을 수행했는지 검증
        verify(accountRepository, times(1)).findById(captor.capture()); // findById를 한번만 호출하였는지 검증
        verify(accountRepository, times(0)).save(any()); // save는 한번도 호출되지 않아야 한다.

        assertEquals(455L, captor.getValue());
        assertNotEquals(4555L, captor.getValue());
        assertEquals("65789", account.getAccountNumber());
        assertEquals(AccountStatus.UNREGISTERED, account.getAccountStatus());
    }
}