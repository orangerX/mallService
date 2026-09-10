package com.mall.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.api.ErrorCode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AllowedHttpMethodFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    public AllowedHttpMethodFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String method = request.getMethod();
        if (!HttpMethod.GET.matches(method) && !HttpMethod.POST.matches(method)) {
            response.setHeader(HttpHeaders.ALLOW, "GET, POST");
            JsonSecurityResponseWriter.write(
                    response, objectMapper, HttpStatus.METHOD_NOT_ALLOWED.value(), ErrorCode.METHOD_NOT_ALLOWED
            );
            return;
        }
        filterChain.doFilter(request, response);
    }
}
