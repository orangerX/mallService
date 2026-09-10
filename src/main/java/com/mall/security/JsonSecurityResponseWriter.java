package com.mall.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.api.ApiResponse;
import com.mall.common.api.ErrorCode;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class JsonSecurityResponseWriter {

    private JsonSecurityResponseWriter() {
    }

    public static void write(HttpServletResponse response, ObjectMapper objectMapper,
                             int status, ErrorCode errorCode) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(errorCode));
    }
}
