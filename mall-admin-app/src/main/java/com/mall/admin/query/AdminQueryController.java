package com.mall.admin.query;

import com.mall.common.api.ApiResponse;
import com.mall.common.api.PageResponse;
import com.mall.user.dto.AdminUserResponse;
import com.mall.user.service.AdminUserQueryService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@Validated
@RestController
@RequestMapping("/admin/api")
public class AdminQueryController {
    private final AdminUserQueryService users;

    public AdminQueryController(AdminUserQueryService users) {
        this.users = users;
    }

    @GetMapping("/users")
    public ApiResponse<PageResponse<AdminUserResponse>> users(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @Min(0) @Max(1) Integer status,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(users.page(keyword, status, page, size));
    }

}
