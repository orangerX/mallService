package com.mall.admin.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.admin.auth.session.AdminSessionService;
import com.mall.common.api.ErrorCode;
import com.mall.security.JsonSecurityResponseWriter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AdminJwtAuthenticationFilter extends OncePerRequestFilter {
    private final AdminJwtTokenService tokens;
    private final AdminSessionService sessions;
    private final ObjectMapper objectMapper;
    public AdminJwtAuthenticationFilter(AdminJwtTokenService tokens, AdminSessionService sessions,
                                        ObjectMapper objectMapper) {
        this.tokens = tokens; this.sessions = sessions; this.objectMapper = objectMapper;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || header.trim().isEmpty()) { chain.doFilter(request, response); return; }
        if (!header.regionMatches(true, 0, "Bearer ", 0, 7)) { reject(response); return; }
        try {
            AdminTokenClaims claims = tokens.parseAccess(header.substring(7).trim());
            if (!sessions.active(claims.getSessionId(), claims.getAdminId())) { reject(response); return; }
            AdminPrincipal principal = new AdminPrincipal(claims.getAdminId(), claims.getUsername(), claims.getSessionId());
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    principal, header.substring(7).trim(), AuthorityUtils.NO_AUTHORITIES));
            chain.doFilter(request, response);
        } catch (AdminTokenValidationException exception) {
            SecurityContextHolder.clearContext(); reject(response);
        }
    }
    private void reject(HttpServletResponse response) throws IOException {
        JsonSecurityResponseWriter.write(response, objectMapper, HttpStatus.UNAUTHORIZED.value(), ErrorCode.UNAUTHORIZED);
    }
}
