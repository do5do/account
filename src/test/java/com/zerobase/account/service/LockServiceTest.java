package com.zerobase.account.service;

import com.zerobase.account.exception.AccountException;
import com.zerobase.account.type.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class LockServiceTest {
    @Mock
    RedissonClient redissonClient;

    // RLock은 직접 생성한것은 아니니만 rLock의 동작에 따라 로직이 변하기 때문에 목으로 만들어준다.
    @Mock
    RLock rLock;

    @InjectMocks
    LockService lockService;

    @Test
    void successGetLock() throws InterruptedException {
        // given
        given(redissonClient.getLock(anyString()))
                .willReturn(rLock);

        given(rLock.tryLock(anyLong(), anyLong(), any()))
                .willReturn(true);

        // when
        // then
        assertDoesNotThrow(() -> lockService.lock("1234"));
    }

    @Test
    void failGetLock() throws InterruptedException {
        // given
        given(redissonClient.getLock(anyString()))
                .willReturn(rLock);

        given(rLock.tryLock(anyLong(), anyLong(), any()))
                .willReturn(false);

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> lockService.lock("1234"));

        // then
        assertEquals(ErrorCode.ACCOUNT_TRANSACTION_LOCK, exception.getErrorCode());
    }
}