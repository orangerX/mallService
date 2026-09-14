package com.mall.auth.service;

import com.mall.auth.dto.LoginRequest;
import com.mall.auth.dto.RefreshTokenRequest;
import com.mall.auth.dto.RegisterRequest;
import com.mall.auth.dto.TokenResponse;
import com.mall.auth.session.AuthSessionService;
import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.security.AuthenticatedUser;
import com.mall.security.JwtTokenService;
import com.mall.security.MallUserDetails;
import com.mall.security.TokenClaims;
import com.mall.user.mapper.UserMapper;
import com.mall.user.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtTokenService tokenService;
    @Mock
    private AuthSessionService sessionService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userMapper, passwordEncoder, authenticationManager, tokenService, sessionService
        );
    }

    @Test
    void registersUserWithEncodedPassword() {
        RegisterRequest request = registerRequest("alice", "password123");
        when(userMapper.findByUsername("alice")).thenReturn(null);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        doAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(9L);
            return 1;
        }).when(userMapper).insert(any(UserEntity.class));

        assertEquals(9L, authService.register(request).getId());
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void rejectsDuplicateUsername() {
        RegisterRequest request = registerRequest("alice", "password123");
        when(userMapper.findByUsername("alice")).thenReturn(enabledUser(9L, "alice"));

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.register(request));

        assertEquals(ErrorCode.USERNAME_EXISTS, exception.getErrorCode());
    }

    @Test
    void loginCreatesIndependentSessionAndTokenPair() {
        LoginRequest request = new LoginRequest();
        request.setUsername("alice");
        request.setPassword("password123");
        MallUserDetails details = new MallUserDetails(enabledUser(9L, "alice"));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(details, null, Collections.emptyList()));
        when(tokenService.getRefreshTokenTtl()).thenReturn(Duration.ofDays(7));
        when(tokenService.issueAccessToken(any(), anyString(), anyString(), anyString())).thenReturn("access-token");
        when(tokenService.issueRefreshToken(any(), anyString(), anyString(), anyString())).thenReturn("refresh-token");
        when(tokenService.getAccessExpiresInSeconds()).thenReturn(1800L);
        when(tokenService.getRefreshExpiresInSeconds()).thenReturn(604800L);

        TokenResponse response = authService.login(request);

        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        verify(sessionService).create(anyString(), org.mockito.ArgumentMatchers.eq(9L), anyString(),
                org.mockito.ArgumentMatchers.eq(Duration.ofDays(7)));
    }

    @Test
    void twoLoginsCreateDifferentDeviceSessions() {
        LoginRequest request = loginRequest();
        MallUserDetails details = new MallUserDetails(enabledUser(9L, "alice"));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(details, null, Collections.emptyList()));
        when(tokenService.getRefreshTokenTtl()).thenReturn(Duration.ofDays(7));
        when(tokenService.issueAccessToken(any(), anyString(), anyString(), anyString())).thenReturn("access-token");
        when(tokenService.issueRefreshToken(any(), anyString(), anyString(), anyString())).thenReturn("refresh-token");

        authService.login(request);
        authService.login(request);

        org.mockito.ArgumentCaptor<String> sessionIds = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(sessionService, org.mockito.Mockito.times(2)).create(
                sessionIds.capture(), org.mockito.ArgumentMatchers.eq(9L), anyString(),
                org.mockito.ArgumentMatchers.eq(Duration.ofDays(7))
        );
        assertNotEquals(sessionIds.getAllValues().get(0), sessionIds.getAllValues().get(1));
    }

    @Test
    void rejectsInvalidLoginCredentials() {
        LoginRequest request = loginRequest();
        doThrow(new BadCredentialsException("bad credentials"))
                .when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.login(request));

        assertEquals(ErrorCode.INVALID_CREDENTIALS, exception.getErrorCode());
    }

    @Test
    void rejectsDisabledAccountLogin() {
        LoginRequest request = loginRequest();
        doThrow(new DisabledException("disabled"))
                .when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.login(request));

        assertEquals(ErrorCode.ACCOUNT_DISABLED, exception.getErrorCode());
    }

    @Test
    void rejectsRefreshTokenReplay() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("refresh-token");
        when(tokenService.parseRefreshToken("refresh-token"))
                .thenReturn(new TokenClaims(9L, "alice", "session-1", "refresh-1", "refresh"));
        when(userMapper.findById(9L)).thenReturn(enabledUser(9L, "alice"));
        when(tokenService.getRefreshTokenTtl()).thenReturn(Duration.ofDays(7));
        when(sessionService.rotateRefreshToken(anyString(), any(), anyString(), anyString(), any()))
                .thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.refresh(request));

        assertEquals(ErrorCode.TOKEN_REFRESH_REJECTED, exception.getErrorCode());
    }

    @Test
    void logoutOnlyDeletesCurrentSession() {
        AuthenticatedUser user = new AuthenticatedUser(9L, "alice", "session-current", "access-1");

        authService.logout(user);

        verify(sessionService).delete("session-current");
        assertNotNull(user.getUserId());
    }

    private static RegisterRequest registerRequest(String username, String password) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }

    private static LoginRequest loginRequest() {
        LoginRequest request = new LoginRequest();
        request.setUsername("alice");
        request.setPassword("password123");
        return request;
    }

    private static UserEntity enabledUser(Long id, String username) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setPasswordHash("encoded-password");
        user.setStatus(UserEntity.STATUS_ENABLED);
        return user;
    }
}
