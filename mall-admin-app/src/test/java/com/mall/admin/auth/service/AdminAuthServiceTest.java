package com.mall.admin.auth.service;

import com.mall.admin.auth.dto.AdminLoginRequest;
import com.mall.admin.auth.dto.AdminRefreshRequest;
import com.mall.admin.auth.session.AdminSessionService;
import com.mall.admin.mapper.AdminMapper;
import com.mall.admin.model.AdminEntity;
import com.mall.admin.security.AdminJwtTokenService;
import com.mall.admin.security.AdminPrincipal;
import com.mall.admin.security.AdminTokenClaims;
import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuthServiceTest {
    @Mock AdminMapper mapper;
    @Mock PasswordEncoder encoder;
    @Mock AdminJwtTokenService tokens;
    @Mock AdminSessionService sessions;
    private AdminAuthService service;

    @BeforeEach
    void setUp() { service = new AdminAuthService(mapper, encoder, tokens, sessions); }

    @Test
    void loginCreatesAdminSession() {
        AdminEntity admin = admin(1);
        when(mapper.findByUsername("admin")).thenReturn(admin);
        when(encoder.matches("123456", "hash")).thenReturn(true);
        when(tokens.refreshTtl()).thenReturn(Duration.ofDays(7));
        when(tokens.issueAccess(org.mockito.ArgumentMatchers.eq(1L), anyString(), anyString(), anyString()))
                .thenReturn("access");
        when(tokens.issueRefresh(org.mockito.ArgumentMatchers.eq(1L), anyString(), anyString(), anyString()))
                .thenReturn("refresh");
        assertEquals("access", service.login(login()).getAccessToken());
        verify(sessions).create(anyString(), org.mockito.ArgumentMatchers.eq(1L), anyString(),
                org.mockito.ArgumentMatchers.eq(Duration.ofDays(7)));
    }

    @Test
    void rejectsDisabledAdmin() {
        AdminEntity admin = admin(0);
        when(mapper.findByUsername("admin")).thenReturn(admin);
        when(encoder.matches("123456", "hash")).thenReturn(true);
        BusinessException exception = assertThrows(BusinessException.class, () -> service.login(login()));
        assertEquals(ErrorCode.ACCOUNT_DISABLED, exception.getErrorCode());
    }

    @Test
    void refreshTokenCanOnlyRotateOnce() {
        AdminRefreshRequest request = new AdminRefreshRequest();
        request.setRefreshToken("refresh");
        when(tokens.parseRefresh("refresh")).thenReturn(new AdminTokenClaims(1L, "admin", "sid", "jti"));
        when(mapper.findById(1L)).thenReturn(admin(1));
        when(tokens.refreshTtl()).thenReturn(Duration.ofDays(7));
        when(sessions.rotate(org.mockito.ArgumentMatchers.eq("sid"), org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq("jti"), anyString(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(false);
        BusinessException exception = assertThrows(BusinessException.class, () -> service.refresh(request));
        assertEquals(ErrorCode.TOKEN_REFRESH_REJECTED, exception.getErrorCode());
    }

    @Test
    void logoutDeletesOnlyCurrentAdminSession() {
        service.logout(new AdminPrincipal(1L, "admin", "sid-current"));
        verify(sessions).delete("sid-current");
    }

    private static AdminLoginRequest login() {
        AdminLoginRequest request = new AdminLoginRequest();
        request.setUsername("admin"); request.setPassword("123456"); return request;
    }
    private static AdminEntity admin(int status) {
        AdminEntity admin = new AdminEntity();
        admin.setId(1L); admin.setUsername("admin"); admin.setPasswordHash("hash"); admin.setStatus(status);
        return admin;
    }
}
