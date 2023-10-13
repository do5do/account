package com.zerobase.account.controller;

import com.zerobase.account.dto.AccountInfo;
import com.zerobase.account.dto.CreateAccount;
import com.zerobase.account.dto.DeleteAccount;
import com.zerobase.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;

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

    @GetMapping("/account")
    public List<AccountInfo> getAccountsByUserId(
            @RequestParam("user_id") Long userId) {
        return accountService.getAccountsByUserId(userId)
                .stream().map(AccountInfo::from)
                .toList();
    }

}
