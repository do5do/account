package com.zerobase.account.controller;

import com.zerobase.account.domain.Account;
import com.zerobase.account.dto.CreateAccount;
import com.zerobase.account.dto.DeleteAccount;
import com.zerobase.account.service.AccountService;
import com.zerobase.account.service.RedisTestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hibernate.sql.Delete;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;
    private final RedisTestService redisTestService;

    @PostMapping("/account")
    public CreateAccount.Response createAccount(
            @RequestBody @Valid CreateAccount.Request request) {
        return CreateAccount.Response.from(
                accountService.createAccount(request.getUserId(),
                        request.getInitialBalance()));
    }

    @DeleteMapping("/account")
    public DeleteAccount.Response deleteAccount(
            @RequestBody @Valid DeleteAccount.Request request) {
        return DeleteAccount.Response.from(
                accountService.deleteAccount(request.getUserId(),
                        request.getAccountNumber()));
    }

    @GetMapping("/account/{id}")
    public Account getAccount(@PathVariable Long id){
        return accountService.getAccount(id);
    }

    @GetMapping("/get-lock")
    public String getLock() {
        return redisTestService.getLock();
    }
}
