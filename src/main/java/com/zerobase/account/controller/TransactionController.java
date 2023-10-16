package com.zerobase.account.controller;

import com.zerobase.account.aop.AccountLock;
import com.zerobase.account.dto.CancelBalance;
import com.zerobase.account.dto.QueryTransactionResponse;
import com.zerobase.account.dto.UseBalance;
import com.zerobase.account.exception.AccountException;
import com.zerobase.account.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 잔액 관련 컨트롤러
 * 1. 잔액 사용
 * 2. 잔액 사용 취소
 * 3. 거래 확인
 */
@Slf4j
@RequiredArgsConstructor
@RestController
public class TransactionController {
    private final TransactionService transactionService;

    @PostMapping("/transaction/use")
    @AccountLock
    public UseBalance.Response useBalance(@RequestBody @Valid UseBalance.Request request) {
        try {
            return UseBalance.Response
                    .from(transactionService.useBalance(request.getUserId(),
                            request.getAccountNumber(),
                            request.getAmount()));
        } catch (AccountException e) {
            log.error("Failed to use balance. msg = {}", e.getMessage());

            transactionService.saveFailedUseTransaction(
                    request.getAccountNumber(),
                    request.getAmount());

            throw e;
        }
    }

    @PostMapping("/transaction/cancel")
    @AccountLock
    public CancelBalance.Response cancelBalance(@RequestBody @Valid CancelBalance.Request request) {
        try {
            return CancelBalance.Response
                    .from(transactionService.cancelBalance(request.getTransactionId(),
                            request.getAccountNumber(),
                            request.getAmount()));
        } catch (AccountException e) {
            log.error("Failed to use balance. msg = {}", e.getMessage());

            transactionService.saveFailedCancelTransaction(
                    request.getAccountNumber(),
                    request.getAmount());

            throw e;
        }
    }

    @GetMapping("/transaction/{transactionId}")
    public QueryTransactionResponse queryTransaction(@PathVariable String transactionId) {
        return QueryTransactionResponse.from(transactionService.queryTransaction(transactionId));
    }
}
