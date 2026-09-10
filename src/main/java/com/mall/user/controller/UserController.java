package com.mall.user.controller;

import com.mall.common.api.ApiResponse;
import com.mall.security.AuthenticatedUser;
import com.mall.user.dto.UserResponse;
import com.mall.user.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(userService.getCurrentUser(user.getUserId()));
    }
}
