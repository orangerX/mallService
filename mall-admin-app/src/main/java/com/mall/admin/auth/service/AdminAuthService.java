package com.mall.admin.auth.service;

import com.mall.admin.auth.dto.AdminLoginRequest;
import com.mall.admin.auth.dto.AdminMeResponse;
import com.mall.admin.auth.dto.AdminRefreshRequest;
import com.mall.admin.auth.dto.AdminTokenResponse;
import com.mall.admin.auth.session.AdminSessionService;
import com.mall.admin.mapper.AdminMapper;
import com.mall.admin.model.AdminEntity;
import com.mall.admin.security.AdminJwtTokenService;
import com.mall.admin.security.AdminPrincipal;
import com.mall.admin.security.AdminTokenClaims;
import com.mall.admin.security.AdminTokenValidationException;
import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AdminAuthService {
    private final AdminMapper mapper;
    private final PasswordEncoder encoder;
    private final AdminJwtTokenService tokens;
    private final AdminSessionService sessions;

    public AdminAuthService(AdminMapper mapper, PasswordEncoder encoder, AdminJwtTokenService tokens,
                            AdminSessionService sessions) {
        this.mapper = mapper; this.encoder = encoder; this.tokens = tokens; this.sessions = sessions;
    }

    public AdminTokenResponse login(AdminLoginRequest request) {
        AdminEntity admin = mapper.findByUsername(request.getUsername().trim());
        if (admin == null || !encoder.matches(request.getPassword(), admin.getPasswordHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_CREDENTIALS);
        }
        if (AdminEntity.STATUS_ENABLED != admin.getStatus()) {
            throw new BusinessException(HttpStatus.FORBIDDEN, ErrorCode.ACCOUNT_DISABLED);
        }
        String sid = UUID.randomUUID().toString();
        String refreshId = UUID.randomUUID().toString();
        sessions.create(sid, admin.getId(), refreshId, tokens.refreshTtl());
        return issue(admin, sid, refreshId);
    }

    public AdminTokenResponse refresh(AdminRefreshRequest request) {
        try {
            AdminTokenClaims claims = tokens.parseRefresh(request.getRefreshToken());
            AdminEntity admin = mapper.findById(claims.getAdminId());
            if (admin == null) throw rejected();
            if (AdminEntity.STATUS_ENABLED != admin.getStatus()) {
                sessions.delete(claims.getSessionId());
                throw new BusinessException(HttpStatus.FORBIDDEN, ErrorCode.ACCOUNT_DISABLED);
            }
            String next = UUID.randomUUID().toString();
            if (!sessions.rotate(claims.getSessionId(), admin.getId(), claims.getTokenId(), next,
                    tokens.refreshTtl())) throw rejected();
            return issue(admin, claims.getSessionId(), next);
        } catch (AdminTokenValidationException exception) {
            throw rejected();
        }
    }

    public void logout(AdminPrincipal principal) { sessions.delete(principal.getSessionId()); }

    public AdminMeResponse me(AdminPrincipal principal) {
        AdminEntity admin = mapper.findById(principal.getAdminId());
        if (admin == null) throw new BusinessException(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED);
        return new AdminMeResponse(admin.getId(), admin.getUsername(), admin.getStatus());
    }

    private AdminTokenResponse issue(AdminEntity admin, String sid, String refreshId) {
        return new AdminTokenResponse(tokens.issueAccess(admin.getId(), admin.getUsername(), sid,
                UUID.randomUUID().toString()), tokens.issueRefresh(admin.getId(), admin.getUsername(), sid,
                refreshId), tokens.accessSeconds(), tokens.refreshSeconds());
    }
    private static BusinessException rejected() {
        return new BusinessException(HttpStatus.UNAUTHORIZED, ErrorCode.TOKEN_REFRESH_REJECTED);
    }
}
