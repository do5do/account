package com.zerobase.account.service;

import com.zerobase.account.domain.Account;
import com.zerobase.account.domain.AccountUser;
import com.zerobase.account.dto.AccountDto;
import com.zerobase.account.exception.AccountException;
import com.zerobase.account.repository.AccountUserRepository;
import com.zerobase.account.type.AccountStatus;
import com.zerobase.account.repository.AccountRepository;
import com.zerobase.account.type.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    void createAccountSuccess() {
        // given
        AccountUser accountUser = AccountUser.builder()
                .id(12L)
                .name("Pobi")
                .build();

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
                accountService.createAccount(1L, 1000L);

        // then
        verify(accountRepository, times(1)).save(captor.capture());

        // willReturn으로 생성한 값만 가지고 있음
        assertEquals(12L, accountDto.getUserId());
        assertEquals("1000000013", captor.getValue().getAccountNumber()); // captor로 캡쳐한 값으로 확인
    }

    @Test
    void createFirstAccount() {
        // given
        AccountUser accountUser = AccountUser.builder()
                .id(15L)
                .name("Pobi")
                .build();

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
                accountService.createAccount(1L, 1000L);

        // then
        verify(accountRepository, times(1)).save(captor.capture());

        // willReturn으로 생성한 값만 가지고 있음
        assertEquals(15L, accountDto.getUserId());
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
    @DisplayName("유저 당 최대 계좌는 10개")
    void createAccountMaxAccountIs10() {
        // given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(AccountUser.builder()
                        .id(1L)
                        .name("Pobi")
                        .build()));

        given(accountRepository.countByAccountUser(any()))
                .willReturn(10);

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.createAccount(1L, 1000L));

        // then
        assertEquals(ErrorCode.MAX_ACCOUNT_PER_USER_10, exception.getErrorCode());
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
        Account account = accountService.getAccount(455L); // anyLong 이기때문에 아무 숫자나 넣어도 된다.

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