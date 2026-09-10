package com.mall.security;

import com.mall.auth.controller.AuthController;
import com.mall.auth.service.AuthService;
import com.mall.auth.session.AuthSessionService;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, UserController.class})
@Import(SecurityConfig.class)
class SecurityAndMethodPolicyMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;
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
