package com.zerobase.account.domain;

import com.zerobase.account.type.TransactionResultType;
import com.zerobase.account.type.TransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class Transaction extends BaseTimeEntity { // 거래
    @Id
    @GeneratedValue
    private Long id;

    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    private TransactionResultType transactionResultType;

    @ManyToOne
    private Account account;

    private Long amount; // 거래 금액

    private Long balanceSnapshot; // 거래 후 계좌 잔액

    private String transactionId;

    private LocalDateTime transactedAt; // 거래 일시
}
