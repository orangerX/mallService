package com.mall.security;

import com.mall.auth.controller.AuthController;
import com.mall.auth.service.AuthService;
import com.mall.auth.session.AuthSessionService;
import com.mall.banner.controller.BannerController;
import com.mall.banner.dto.BannerResponse;
import com.mall.banner.service.BannerService;
import com.mall.cart.controller.CartController;
import com.mall.cart.service.CartService;
import com.mall.order.controller.OrderController;
import com.mall.order.service.OrderService;
import com.mall.product.controller.CategoryController;
import com.mall.product.controller.ProductController;
import com.mall.product.dto.CategoryResponse;
import com.mall.product.dto.ProductResponse;
import com.mall.product.dto.ProductSkuResponse;
import com.mall.product.service.CategoryService;
import com.mall.product.service.ProductService;
import com.mall.user.controller.UserController;
import com.mall.user.dto.UserResponse;
import com.mall.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        AuthController.class,
        BannerController.class,
        CartController.class,
        CategoryController.class,
        OrderController.class,
        ProductController.class,
        UserController.class
})
@Import(SecurityConfig.class)
class SecurityAndMethodPolicyMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;
    @MockBean
    private BannerService bannerService;
    @MockBean
    private CartService cartService;
    @MockBean
    private OrderService orderService;
    @MockBean
    private CategoryService categoryService;
    @MockBean
    private ProductService productService;
    @MockBean
    private UserService userService;
    @MockBean
    private CustomUserDetailsService userDetailsService;
    @MockBean
    private JwtTokenService tokenService;
    @MockBean
    private AuthSessionService sessionService;

    @Test
    void protectedEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void cartRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void ordersRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void validBearerTokenAuthenticatesRequest() throws Exception {
        TokenClaims claims = new TokenClaims(9L, "alice", "session-1", "access-1", "access");
        when(tokenService.parseAccessToken("valid-token")).thenReturn(claims);
        when(sessionService.isActive("session-1", 9L)).thenReturn(true);
        when(userService.getCurrentUser(9L)).thenReturn(new UserResponse(9L, "alice", 1, null));

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.username").value("alice"));
    }

    @Test
    void invalidRegistrationBodyReturnsValidationError() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void homeBannersArePublicAndContainAbsoluteImageUrls() throws Exception {
        when(bannerService.listHomeBanners("http://localhost")).thenReturn(Collections.singletonList(
                new BannerResponse(
                        1L,
                        "秋日石榴",
                        "http://localhost/images/banners/autumn-pomegranate.jpg",
                        1
                )
        ));

        mockMvc.perform(get("/api/banners/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data[0].imageUrl")
                        .value("http://localhost/images/banners/autumn-pomegranate.jpg"));
    }

    @Test
    void homeCategoriesArePublic() throws Exception {
        when(categoryService.listHomeCategories()).thenReturn(Collections.singletonList(
                new CategoryResponse(1L, "新鲜热卖", "fresh-hot", 1)
        ));

        mockMvc.perform(get("/api/categories/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data[0].name").value("新鲜热卖"));
    }

    @Test
    void productsArePublicAndBindCategoryQueryParameter() throws Exception {
        when(productService.listByCategory(1L, "http://localhost")).thenReturn(Collections.singletonList(
                new ProductResponse(
                        1L,
                        1L,
                        "当季软籽石榴",
                        "果粒饱满，籽软汁多",
                        "http://localhost/images/products/soft-seed-pomegranate.jpg",
                        2L,
                        new BigDecimal("29.90"),
                        new BigDecimal("35.88"),
                        "约2.5kg/箱",
                        200,
                        128,
                        Collections.singletonList(new ProductSkuResponse(
                                2L, "SKU-000001-M", "1kg标准装", new BigDecimal("29.90"),
                                new BigDecimal("35.88"), "1kg/份", 200, 128, true
                        ))
                )
        ));

        mockMvc.perform(get("/api/products").queryParam("categoryId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data[0].name").value("当季软籽石榴"))
                .andExpect(jsonPath("$.data[0].originalPrice").value(35.88))
                .andExpect(jsonPath("$.data[0].defaultSkuId").value(2))
                .andExpect(jsonPath("$.data[0].skus[0].skuName").value("1kg标准装"));
    }

    @Test
    void productCategoryQueryParameterIsRequiredAndPositive() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/api/products").queryParam("categoryId", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void rejectsAllMethodsOtherThanGetAndPost() throws Exception {
        for (org.springframework.http.HttpMethod method : Arrays.asList(
                org.springframework.http.HttpMethod.PUT,
                org.springframework.http.HttpMethod.DELETE,
                org.springframework.http.HttpMethod.PATCH,
                org.springframework.http.HttpMethod.HEAD,
                org.springframework.http.HttpMethod.OPTIONS
        )) {
            mockMvc.perform(request(method, "/api/auth/login"))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(header().string(HttpHeaders.ALLOW, "GET, POST"))
                    .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
        }
    }

    @Test
    void methodMismatchStillUsesStandardJsonError() throws Exception {
        mockMvc.perform(get("/api/auth/login"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string(HttpHeaders.ALLOW, "GET, POST"))
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }
}
