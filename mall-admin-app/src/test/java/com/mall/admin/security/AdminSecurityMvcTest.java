package com.mall.admin.security;

import com.mall.admin.auth.controller.AdminAuthController;
import com.mall.admin.auth.service.AdminAuthService;
import com.mall.admin.auth.session.AdminSessionService;
import com.mall.admin.category.AdminCategoryController;
import com.mall.admin.config.AdminSecurityConfig;
import com.mall.admin.coupon.AdminCouponController;
import com.mall.admin.order.AdminOrderController;
import com.mall.admin.product.AdminProductController;
import com.mall.admin.query.AdminQueryController;
import com.mall.admin.user.AdminUserController;
import com.mall.common.api.PageResponse;
import com.mall.coupon.service.AdminCouponQueryService;
import com.mall.coupon.service.AdminCouponService;
import com.mall.coupon.model.CouponEntity;
import com.mall.order.service.AdminOrderQueryService;
import com.mall.order.dto.AdminOrderResponse;
import com.mall.product.dto.AdminProductDetailResponse;
import com.mall.product.dto.AdminProductResponse;
import com.mall.product.model.CategoryEntity;
import com.mall.product.service.AdminCategoryService;
import com.mall.product.service.AdminProductQueryService;
import com.mall.product.service.AdminProductService;
import com.mall.user.service.AdminUserQueryService;
import com.mall.user.service.AdminUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AdminAuthController.class, AdminCategoryController.class,
        AdminProductController.class, AdminOrderController.class, AdminCouponController.class,
        AdminQueryController.class, AdminUserController.class})
@Import(AdminSecurityConfig.class)
class AdminSecurityMvcTest {
    @Autowired MockMvc mvc;
    @MockBean AdminAuthService auth;
    @MockBean AdminProductQueryService products;
    @MockBean AdminProductService productManagement;
    @MockBean AdminCategoryService categories;
    @MockBean AdminOrderQueryService orders;
    @MockBean AdminUserQueryService users;
    @MockBean AdminUserService userManagement;
    @MockBean AdminCouponQueryService coupons;
    @MockBean AdminCouponService couponManagement;
    @MockBean AdminJwtTokenService tokens;
    @MockBean AdminSessionService sessions;

    @Test
    void queriesRequireAdminToken() throws Exception {
        mvc.perform(get("/admin/api/products")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void authenticatedPaginationAndLimitsWork() throws Exception {
        when(tokens.parseAccess("admin-token")).thenReturn(new AdminTokenClaims(1L, "admin", "sid", "jti"));
        when(sessions.active("sid", 1L)).thenReturn(true);
        when(products.page(isNull(), isNull(), isNull(), anyInt(), anyInt(), anyString()))
                .thenReturn(new PageResponse<AdminProductResponse>(Collections.emptyList(), 1, 20, 0));
        mvc.perform(get("/admin/api/products").header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(20));
        mvc.perform(get("/admin/api/products").queryParam("size", "101")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void disallowedMethodsReturnStandard405() throws Exception {
        for (HttpMethod method : new HttpMethod[]{HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.PATCH,
                HttpMethod.HEAD, HttpMethod.OPTIONS}) {
            mvc.perform(request(method, "/admin/api/auth/login"))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(header().string(HttpHeaders.ALLOW, "GET, POST"))
                    .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
        }
    }

    @Test
    void authenticatedAdminCanCreateCategory() throws Exception {
        when(tokens.parseAccess("admin-token")).thenReturn(new AdminTokenClaims(1L, "admin", "sid", "jti"));
        when(sessions.active("sid", 1L)).thenReturn(true);
        CategoryEntity category = new CategoryEntity();
        category.setId(8L);
        category.setName("鲜果礼盒");
        category.setCode("gift-fruit");
        category.setSortOrder(6);
        category.setStatus(1);
        when(categories.create(any())).thenReturn(category);

        mvc.perform(post("/admin/api/categories/create")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"鲜果礼盒\",\"code\":\"gift-fruit\",\"sortOrder\":6,\"status\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(8))
                .andExpect(jsonPath("$.data.code").value("gift-fruit"));
    }

    @Test
    void invalidCategoryCodeReturns400() throws Exception {
        when(tokens.parseAccess("admin-token")).thenReturn(new AdminTokenClaims(1L, "admin", "sid", "jti"));
        when(sessions.active("sid", 1L)).thenReturn(true);

        mvc.perform(post("/admin/api/categories/create")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"鲜果礼盒\",\"code\":\"Gift Fruit\",\"sortOrder\":6,\"status\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void authenticatedAdminCanCreateProductWithSku() throws Exception {
        when(tokens.parseAccess("admin-token")).thenReturn(new AdminTokenClaims(1L, "admin", "sid", "jti"));
        when(sessions.active("sid", 1L)).thenReturn(true);
        when(productManagement.create(any())).thenReturn(20L);
        AdminProductResponse product = new AdminProductResponse();
        product.setId(20L);
        product.setName("测试苹果");
        when(products.detail(org.mockito.ArgumentMatchers.eq(20L), anyString()))
                .thenReturn(new AdminProductDetailResponse(product, Collections.emptyList()));

        mvc.perform(post("/admin/api/products/create")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":1,\"name\":\"测试苹果\",\"subtitle\":\"清甜爽脆\"," +
                                "\"imagePath\":\"/images/products/apple.jpg\",\"sortOrder\":1,\"status\":1," +
                                "\"skus\":[{\"skuCode\":\"APPLE-1KG\",\"skuName\":\"1kg装\"," +
                                "\"price\":19.90,\"unit\":\"1kg/份\",\"stock\":100," +
                                "\"defaultSku\":true,\"sortOrder\":1,\"status\":1}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.product.id").value(20));
    }

    @Test
    void invalidSkuPriceReturns400() throws Exception {
        when(tokens.parseAccess("admin-token")).thenReturn(new AdminTokenClaims(1L, "admin", "sid", "jti"));
        when(sessions.active("sid", 1L)).thenReturn(true);

        mvc.perform(post("/admin/api/products/skus/create")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1,\"sku\":{\"skuCode\":\"BAD-SKU\"," +
                                "\"skuName\":\"无效规格\",\"price\":0,\"unit\":\"1kg/份\"," +
                                "\"stock\":10,\"defaultSku\":false,\"sortOrder\":1,\"status\":1}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void authenticatedAdminCanPageOrders() throws Exception {
        when(tokens.parseAccess("admin-token")).thenReturn(new AdminTokenClaims(1L, "admin", "sid", "jti"));
        when(sessions.active("sid", 1L)).thenReturn(true);
        when(orders.page("202609", 2L, 1, 2, 10))
                .thenReturn(new PageResponse<AdminOrderResponse>(Collections.emptyList(), 2, 10, 12));

        mvc.perform(get("/admin/api/orders")
                        .queryParam("orderNo", "202609")
                        .queryParam("userId", "2")
                        .queryParam("status", "1")
                        .queryParam("page", "2")
                        .queryParam("size", "10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.total").value(12));
    }

    @Test
    void authenticatedAdminCanCreateCoupon() throws Exception {
        when(tokens.parseAccess("admin-token")).thenReturn(new AdminTokenClaims(1L, "admin", "sid", "jti"));
        when(sessions.active("sid", 1L)).thenReturn(true);
        CouponEntity coupon = new CouponEntity();
        coupon.setId(9L);
        coupon.setName("周末鲜果券");
        coupon.setThresholdAmount(new java.math.BigDecimal("100.00"));
        coupon.setDiscountAmount(new java.math.BigDecimal("10.00"));
        when(couponManagement.create(any())).thenReturn(coupon);

        mvc.perform(post("/admin/api/coupons/create")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"周末鲜果券\",\"thresholdAmount\":100," +
                                "\"discountAmount\":10,\"totalQuantity\":500," +
                                "\"startAt\":\"2026-09-01T00:00:00\"," +
                                "\"endAt\":\"2026-10-01T00:00:00\"," +
                                "\"status\":1,\"sortOrder\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(9))
                .andExpect(jsonPath("$.data.discountAmount").value(10));
    }

    @Test
    void authenticatedAdminCanCreateMallUser() throws Exception {
        when(tokens.parseAccess("admin-token")).thenReturn(new AdminTokenClaims(1L, "admin", "sid", "jti"));
        when(sessions.active("sid", 1L)).thenReturn(true);
        com.mall.user.dto.AdminUserResponse user = new com.mall.user.dto.AdminUserResponse();
        user.setId(8L);
        user.setUsername("orange_user");
        user.setStatus(1);
        when(userManagement.create(any())).thenReturn(user);

        mvc.perform(post("/admin/api/users/create")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"orange_user\",\"status\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(8))
                .andExpect(jsonPath("$.data.username").value("orange_user"));
    }

    @Test
    void invalidCouponQuantityReturns400() throws Exception {
        when(tokens.parseAccess("admin-token")).thenReturn(new AdminTokenClaims(1L, "admin", "sid", "jti"));
        when(sessions.active("sid", 1L)).thenReturn(true);

        mvc.perform(post("/admin/api/coupons/create")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"无效券\",\"thresholdAmount\":100," +
                                "\"discountAmount\":10,\"totalQuantity\":0," +
                                "\"startAt\":\"2026-09-01T00:00:00\"," +
                                "\"endAt\":\"2026-10-01T00:00:00\"," +
                                "\"status\":1,\"sortOrder\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
