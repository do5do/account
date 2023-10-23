package com.zerobase.account.exception;


import com.zerobase.account.type.ErrorCode;
import lombok.Getter;

@Getter
public class AccountException extends RuntimeException {
    private final ErrorCode errorCode;
    private final String errorMessage;

    public AccountException(ErrorCode errorCode) {
        super(errorCode.getDescription());
        this.errorCode = errorCode;
        this.errorMessage = errorCode.getDescription();
    }
}
