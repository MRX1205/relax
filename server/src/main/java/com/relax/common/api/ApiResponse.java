package com.relax.common.api;

import org.slf4j.MDC;

public record ApiResponse<T>(String code, String message, T data, String requestId) {

    private static final String REQUEST_ID_KEY = "requestId";

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("OK", "", data, MDC.get(REQUEST_ID_KEY));
    }

    public static ApiResponse<Void> failure(String code, String message) {
        return new ApiResponse<>(code, message, null, MDC.get(REQUEST_ID_KEY));
    }
}

