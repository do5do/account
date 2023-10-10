package com.zerobase.account.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerobase.account.domain.Account;
import com.zerobase.account.dto.AccountDto;
import com.zerobase.account.dto.CreateAccount;
import com.zerobase.account.dto.DeleteAccount;
import com.zerobase.account.type.AccountStatus;
import com.zerobase.account.service.AccountService;
import com.zerobase.account.service.RedisTestService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class) // test 하려는 컨트롤러 명시
class AccountControllerTest {
    // AccountController가 의존하는 객체를 가짜 객체로 생성하여 자동 주입
    @MockBean
    AccountService accountService;

    @MockBean
    RedisTestService redisTestService;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void successCreateAccount() throws Exception {
        // given
        given(accountService.createAccount(anyLong(), anyLong())) // 요청값
                // 응답값
                .willReturn(AccountDto.builder()
                        .userId(1L)
                        .accountNumber("1234567890")
                        .registeredAt(LocalDateTime.now())
                        .unRegisteredAt(LocalDateTime.now())
                        .build());
        // when
        // then
        mockMvc.perform(post("/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateAccount.Request(1L, 100L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1)) // response body의 json
                .andExpect(jsonPath("$.accountNumber").value("1234567890"))
                .andDo(print()); // 호출 한 뒤 하는 동작
    }

    @Test
    void successDeleteAccount() throws Exception {
        // given
        given(accountService.deleteAccount(anyLong(), anyString())) // 요청값
                // 응답값
                .willReturn(AccountDto.builder()
                        .userId(1L)
                        .accountNumber("1234567890")
                        .registeredAt(LocalDateTime.now())
                        .unRegisteredAt(LocalDateTime.now())
                        .build());
        // when
        // then
        mockMvc.perform(delete("/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new DeleteAccount.Request(1L, "1000000000"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.accountNumber").value("1234567890"))
                .andDo(print());
    }

    @Test
    void successGetAccount() throws Exception {
        // given
        List<AccountDto> accountDtos = getAccountDtos();

        given(accountService.getAccountsByUserId(anyLong()))
                .willReturn(accountDtos);

        // when
        // then
        mockMvc.perform(get("/account?user_id=1"))
                .andDo(print())
                .andExpect(jsonPath("$[0].accountNumber").value("1000000011"))
                .andExpect(jsonPath("$[0].balance").value(10L))
                .andExpect(jsonPath("$[1].accountNumber").value("1000000151"))
                .andExpect(jsonPath("$[1].balance").value(100L))
                .andExpect(jsonPath("$[2].accountNumber").value("1000001123"))
                .andExpect(jsonPath("$[2].balance").value(1000L));
    }

    private static List<AccountDto> getAccountDtos() {
        return List.of(AccountDto.builder().balance(10L)
                        .accountNumber("1000000011").build(),
                AccountDto.builder().balance(100L)
                        .accountNumber("1000000151").build(),
                AccountDto.builder().balance(1000L)
                        .accountNumber("1000001123").build()
        );
    }
}