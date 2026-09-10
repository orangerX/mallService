package com.mall.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.auth.session.AuthSessionService;
import com.mall.common.api.ErrorCode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService tokenService;
    private final AuthSessionService sessionService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtTokenService tokenService, AuthSessionService sessionService,
                                   ObjectMapper objectMapper) {
        this.tokenService = tokenService;
        this.sessionService = sessionService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || authorization.trim().isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            reject(response);
            return;
        }

        try {
            String token = authorization.substring(BEARER_PREFIX.length()).trim();
            TokenClaims claims = tokenService.parseAccessToken(token);
            if (!sessionService.isActive(claims.getSessionId(), claims.getUserId())) {
                reject(response);
                return;
            }

            AuthenticatedUser principal = new AuthenticatedUser(
                    claims.getUserId(), claims.getUsername(), claims.getSessionId(), claims.getTokenId()
            );
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal, token, AuthorityUtils.NO_AUTHORITIES
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (TokenValidationException exception) {
            SecurityContextHolder.clearContext();
            reject(response);
        }
    }

    private void reject(HttpServletResponse response) throws IOException {
        JsonSecurityResponseWriter.write(
                response, objectMapper, HttpStatus.UNAUTHORIZED.value(), ErrorCode.UNAUTHORIZED
        );
    }
}
