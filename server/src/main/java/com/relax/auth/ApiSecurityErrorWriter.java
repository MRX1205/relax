package com.relax.auth;

import java.io.IOException;

import tools.jackson.databind.ObjectMapper;
import com.relax.common.api.ApiResponse;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class ApiSecurityErrorWriter {

    private final ObjectMapper objectMapper;

    ApiSecurityErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.failure(code, message));
    }
}
