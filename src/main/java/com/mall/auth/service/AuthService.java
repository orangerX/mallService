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
import com.mall.security.TokenValidationException;
import com.mall.user.dto.UserResponse;
import com.mall.user.mapper.UserMapper;
import com.mall.user.model.UserEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService tokenService;
    private final AuthSessionService sessionService;

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtTokenService tokenService,
                       AuthSessionService sessionService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.sessionService = sessionService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();
        validateBcryptPasswordLength(request.getPassword());
        if (userMapper.findByUsername(username) != null) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.USERNAME_EXISTS);
        }

        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserEntity.STATUS_ENABLED);
        try {
            userMapper.insert(user);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.USERNAME_EXISTS);
        }
        return UserResponse.from(user);
    }

    public TokenResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername().trim(), request.getPassword())
            );
            MallUserDetails user = (MallUserDetails) authentication.getPrincipal();
            return createSessionAndTokens(user.getUserId(), user.getUsername());
        } catch (DisabledException exception) {
            throw new BusinessException(HttpStatus.FORBIDDEN, ErrorCode.ACCOUNT_DISABLED);
        } catch (BadCredentialsException exception) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_CREDENTIALS);
        }
    }

    public TokenResponse refresh(RefreshTokenRequest request) {
        try {
            TokenClaims claims = tokenService.parseRefreshToken(request.getRefreshToken());
            UserEntity user = userMapper.findById(claims.getUserId());
            if (user == null) {
                throw new BusinessException(HttpStatus.UNAUTHORIZED, ErrorCode.TOKEN_REFRESH_REJECTED);
            }
            if (UserEntity.STATUS_ENABLED != user.getStatus()) {
                sessionService.delete(claims.getSessionId());
                throw new BusinessException(HttpStatus.FORBIDDEN, ErrorCode.ACCOUNT_DISABLED);
            }

            String newRefreshTokenId = UUID.randomUUID().toString();
            boolean rotated = sessionService.rotateRefreshToken(
                    claims.getSessionId(), claims.getUserId(), claims.getTokenId(),
                    newRefreshTokenId, tokenService.getRefreshTokenTtl()
            );
            if (!rotated) {
                throw new BusinessException(HttpStatus.UNAUTHORIZED, ErrorCode.TOKEN_REFRESH_REJECTED);
            }
            return issueTokenResponse(
                    user.getId(), user.getUsername(), claims.getSessionId(), newRefreshTokenId
            );
        } catch (TokenValidationException exception) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, ErrorCode.TOKEN_REFRESH_REJECTED);
        }
    }

    public void logout(AuthenticatedUser user) {
        sessionService.delete(user.getSessionId());
    }

    private TokenResponse createSessionAndTokens(Long userId, String username) {
        String sessionId = UUID.randomUUID().toString();
        String refreshTokenId = UUID.randomUUID().toString();
        sessionService.create(sessionId, userId, refreshTokenId, tokenService.getRefreshTokenTtl());
        return issueTokenResponse(userId, username, sessionId, refreshTokenId);
    }

    private TokenResponse issueTokenResponse(Long userId, String username, String sessionId,
                                             String refreshTokenId) {
        String accessTokenId = UUID.randomUUID().toString();
        String accessToken = tokenService.issueAccessToken(userId, username, sessionId, accessTokenId);
        String refreshToken = tokenService.issueRefreshToken(userId, username, sessionId, refreshTokenId);
        return new TokenResponse(
                accessToken,
                refreshToken,
                tokenService.getAccessExpiresInSeconds(),
                tokenService.getRefreshExpiresInSeconds()
        );
    }

    private static void validateBcryptPasswordLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, "password: UTF-8 编码后不能超过 72 字节"
            );
        }
    }
}
