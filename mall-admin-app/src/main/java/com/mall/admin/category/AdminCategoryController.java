package com.mall.admin.category;

import com.mall.common.api.ApiResponse;
import com.mall.common.api.PageResponse;
import com.mall.product.dto.AdminCategoryCreateRequest;
import com.mall.product.dto.AdminCategoryIdRequest;
import com.mall.product.dto.AdminCategoryStatusRequest;
import com.mall.product.dto.AdminCategoryUpdateRequest;
import com.mall.product.model.CategoryEntity;
import com.mall.product.service.AdminCategoryService;
import com.mall.product.service.AdminProductQueryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@Validated
@RestController
@RequestMapping("/admin/api/categories")
public class AdminCategoryController {
    private final AdminProductQueryService productQueryService;
    private final AdminCategoryService categoryService;

    public AdminCategoryController(AdminProductQueryService productQueryService,
                                   AdminCategoryService categoryService) {
        this.productQueryService = productQueryService;
        this.categoryService = categoryService;
    }

    @GetMapping
    public ApiResponse<PageResponse<CategoryEntity>> list() {
        List<CategoryEntity> records = productQueryService.categories();
        return ApiResponse.success(new PageResponse<>(records, 1, records.size(), records.size()));
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<CategoryEntity>> create(
            @Valid @RequestBody AdminCategoryCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(categoryService.create(request)));
    }

    @PostMapping("/update")
    public ApiResponse<CategoryEntity> update(@Valid @RequestBody AdminCategoryUpdateRequest request) {
        return ApiResponse.success(categoryService.update(request));
    }

    @PostMapping("/status")
    public ApiResponse<CategoryEntity> changeStatus(@Valid @RequestBody AdminCategoryStatusRequest request) {
        return ApiResponse.success(categoryService.changeStatus(request.getCategoryId(), request.getStatus()));
    }

    @PostMapping("/delete")
    public ApiResponse<Void> delete(@Valid @RequestBody AdminCategoryIdRequest request) {
        categoryService.delete(request.getCategoryId());
        return ApiResponse.success();
    }
}
