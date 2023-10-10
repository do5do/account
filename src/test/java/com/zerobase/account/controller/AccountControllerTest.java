package com.zerobase.account.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerobase.account.domain.Account;
import com.zerobase.account.dto.AccountDto;
import com.zerobase.account.dto.CreateAccount;
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

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.accountNumber").value("1234567890"))
                .andDo(print());
    }

    @Test
    @DisplayName("테스트 에시")
    void testMockito() throws Exception {
        // given
        given(accountService.getAccount(anyLong()))
                .willReturn(Account.builder()
                        .accountNumber("3456")
                        .accountStatus(AccountStatus.IN_USE)
                        .build());

        // when
        // then
        mockMvc.perform(get("/account/876"))
                .andDo(print()) // 호출 한 뒤 하는 동작
                .andExpect(jsonPath("$.accountNumber").value("3456")) // response body의 json
                .andExpect(jsonPath("$.accountStatus").value("IN_USE"))
                .andExpect(status().isOk());
    }
}