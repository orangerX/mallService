package com.mall.admin.user;

import com.mall.common.api.ApiResponse;
import com.mall.user.dto.AdminUserCreateRequest;
import com.mall.user.dto.AdminUserIdRequest;
import com.mall.user.dto.AdminUserResponse;
import com.mall.user.dto.AdminUserStatusRequest;
import com.mall.user.dto.AdminUserUpdateRequest;
import com.mall.user.service.AdminUserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.Min;

@Validated
@RestController
@RequestMapping("/admin/api/users")
public class AdminUserController {
    private final AdminUserService userService;

    public AdminUserController(AdminUserService userService) {
        this.userService = userService;
    }

    @GetMapping("/detail")
    public ApiResponse<AdminUserResponse> detail(@RequestParam @Min(1) Long userId) {
        return ApiResponse.success(userService.detail(userId));
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<AdminUserResponse>> create(
            @Valid @RequestBody AdminUserCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(userService.create(request)));
    }

    @PostMapping("/update")
    public ApiResponse<AdminUserResponse> update(@Valid @RequestBody AdminUserUpdateRequest request) {
        return ApiResponse.success(userService.update(request));
    }

    @PostMapping("/status")
    public ApiResponse<AdminUserResponse> changeStatus(@Valid @RequestBody AdminUserStatusRequest request) {
        return ApiResponse.success(userService.changeStatus(request.getUserId(), request.getStatus()));
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody AdminUserIdRequest request) {
        userService.resetPassword(request.getUserId());
        return ApiResponse.success();
    }

    @PostMapping("/delete")
    public ApiResponse<Void> delete(@Valid @RequestBody AdminUserIdRequest request) {
        userService.delete(request.getUserId());
        return ApiResponse.success();
    }
}
