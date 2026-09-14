package com.mall.admin.auth.controller;

import com.mall.admin.auth.dto.AdminLoginRequest;
import com.mall.admin.auth.dto.AdminMeResponse;
import com.mall.admin.auth.dto.AdminRefreshRequest;
import com.mall.admin.auth.dto.AdminTokenResponse;
import com.mall.admin.auth.service.AdminAuthService;
import com.mall.admin.security.AdminPrincipal;
import com.mall.common.api.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Validated
@RestController
@RequestMapping("/admin/api/auth")
public class AdminAuthController {
    private final AdminAuthService service;
    public AdminAuthController(AdminAuthService service) { this.service = service; }
    @PostMapping("/login")
    public ApiResponse<AdminTokenResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        return ApiResponse.success(service.login(request));
    }
    @PostMapping("/refresh")
    public ApiResponse<AdminTokenResponse> refresh(@Valid @RequestBody AdminRefreshRequest request) {
        return ApiResponse.success(service.refresh(request));
    }
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal AdminPrincipal principal) {
        service.logout(principal); return ApiResponse.success();
    }
    @GetMapping("/me")
    public ApiResponse<AdminMeResponse> me(@AuthenticationPrincipal AdminPrincipal principal) {
        return ApiResponse.success(service.me(principal));
    }
}
