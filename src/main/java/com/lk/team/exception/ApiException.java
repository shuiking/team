package com.lk.team.exception;

import com.lk.team.common.ErrorCode;

/**
 * 自定义异常
 * @Author : lk
 * @create 2023/5/5 17:38
 */
public class ApiException extends RuntimeException{
    private final int code;
    private final String description;

    public ApiException(String message, int code, String description) {
        super(message);
        this.code = code;
        this.description = description;
    }

    public ApiException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.description = errorCode.getDescription();
    }

    public ApiException(ErrorCode errorCode, String description) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
