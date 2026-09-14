package com.mall.admin.product;

import com.mall.common.api.ApiResponse;
import com.mall.common.api.PageResponse;
import com.mall.product.dto.AdminProductCreateRequest;
import com.mall.product.dto.AdminProductDetailResponse;
import com.mall.product.dto.AdminProductIdRequest;
import com.mall.product.dto.AdminProductResponse;
import com.mall.product.dto.AdminProductSkuCreateRequest;
import com.mall.product.dto.AdminProductSkuIdRequest;
import com.mall.product.dto.AdminProductSkuStatusRequest;
import com.mall.product.dto.AdminProductSkuUpdateRequest;
import com.mall.product.dto.AdminProductStatusRequest;
import com.mall.product.dto.AdminProductUpdateRequest;
import com.mall.product.model.ProductSkuEntity;
import com.mall.product.service.AdminProductQueryService;
import com.mall.product.service.AdminProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@Validated
@RestController
@RequestMapping("/admin/api/products")
public class AdminProductController {
    private final AdminProductQueryService queryService;
    private final AdminProductService productService;

    public AdminProductController(AdminProductQueryService queryService, AdminProductService productService) {
        this.queryService = queryService;
        this.productService = productService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminProductResponse>> products(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @Min(1) Long categoryId,
            @RequestParam(required = false) @Min(0) @Max(1) Integer status,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(queryService.page(keyword, categoryId, status, page, size, baseUrl()));
    }

    @GetMapping("/detail")
    public ApiResponse<AdminProductDetailResponse> detail(@RequestParam @Min(1) Long productId) {
        return ApiResponse.success(queryService.detail(productId, baseUrl()));
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<AdminProductDetailResponse>> create(
            @Valid @RequestBody AdminProductCreateRequest request) {
        Long productId = productService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(queryService.detail(productId, baseUrl())));
    }

    @PostMapping("/update")
    public ApiResponse<AdminProductDetailResponse> update(@Valid @RequestBody AdminProductUpdateRequest request) {
        return ApiResponse.success(queryService.detail(productService.update(request), baseUrl()));
    }

    @PostMapping("/status")
    public ApiResponse<AdminProductDetailResponse> changeStatus(
            @Valid @RequestBody AdminProductStatusRequest request) {
        return ApiResponse.success(queryService.detail(
                productService.changeStatus(request.getProductId(), request.getStatus()), baseUrl()));
    }

    @PostMapping("/delete")
    public ApiResponse<Void> delete(@Valid @RequestBody AdminProductIdRequest request) {
        productService.delete(request.getProductId());
        return ApiResponse.success();
    }

    @PostMapping("/skus/create")
    public ResponseEntity<ApiResponse<ProductSkuEntity>> createSku(
            @Valid @RequestBody AdminProductSkuCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(productService.createSku(request)));
    }

    @PostMapping("/skus/update")
    public ApiResponse<ProductSkuEntity> updateSku(@Valid @RequestBody AdminProductSkuUpdateRequest request) {
        return ApiResponse.success(productService.updateSku(request));
    }

    @PostMapping("/skus/status")
    public ApiResponse<ProductSkuEntity> changeSkuStatus(
            @Valid @RequestBody AdminProductSkuStatusRequest request) {
        return ApiResponse.success(productService.changeSkuStatus(request.getSkuId(), request.getStatus()));
    }

    @PostMapping("/skus/set-default")
    public ApiResponse<ProductSkuEntity> setDefaultSku(@Valid @RequestBody AdminProductSkuIdRequest request) {
        return ApiResponse.success(productService.setDefaultSku(request.getSkuId()));
    }

    @PostMapping("/skus/delete")
    public ApiResponse<Void> deleteSku(@Valid @RequestBody AdminProductSkuIdRequest request) {
        productService.deleteSku(request.getSkuId());
        return ApiResponse.success();
    }

    private static String baseUrl() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
    }
}
