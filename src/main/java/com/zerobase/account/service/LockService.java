package com.zerobase.account.service;

import com.zerobase.account.exception.AccountException;
import com.zerobase.account.type.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class LockService {
    private final RedissonClient redissonClient;

    public void lock(String accountNumber) {
        RLock lock = redissonClient.getLock(getLockKey(accountNumber));
        log.debug("Trying lock for accountNumber : {}", accountNumber);

        try {
            // 5초 동안 아무 동작이 없으면 락이 풀리게 되고, 3초동안 기다렸는데 락이 안풀리면 락을 취득하지 못한다.
            boolean isLock = lock.tryLock(3, 5, TimeUnit.SECONDS);
            if (!isLock) {
                log.error("======Lock acquisition failed=====");
                throw new AccountException(ErrorCode.ACCOUNT_TRANSACTION_LOCK);
            }
        } catch (AccountException e) {
            throw e;
        } catch (Exception e) {
            log.error("Redis lock failed", e);
        }
    }

    public void unlock(String accountNumber) {
        log.debug("Unlock for accountNumber : {}", accountNumber);
        redissonClient.getLock(getLockKey(accountNumber)).unlock();
    }

    private static String getLockKey(String accountNumber) {
        return "ACLK:" + accountNumber;
    }
}
