package com.relax.common.api;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public BusinessException(String code, String message) {
        this(HttpStatus.BAD_REQUEST, code, message);
    }

    public BusinessException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
