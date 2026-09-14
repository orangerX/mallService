package com.mall.admin.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.admin.auth.session.AdminSessionService;
import com.mall.admin.security.AdminJwtAuthenticationFilter;
import com.mall.admin.security.AdminJwtTokenService;
import com.mall.common.api.ErrorCode;
import com.mall.security.AllowedHttpMethodFilter;
import com.mall.security.JsonSecurityResponseWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.channel.ChannelProcessingFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class AdminSecurityConfig {
    @Bean
    public PasswordEncoder adminPasswordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http, ObjectMapper objectMapper,
            AdminJwtTokenService tokens, AdminSessionService sessions) throws Exception {
        http.cors().disable().csrf().disable().formLogin().disable().httpBasic().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
                .exceptionHandling()
                .authenticationEntryPoint((request, response, exception) -> JsonSecurityResponseWriter.write(
                        response, objectMapper, HttpStatus.UNAUTHORIZED.value(), ErrorCode.UNAUTHORIZED))
                .accessDeniedHandler((request, response, exception) -> JsonSecurityResponseWriter.write(
                        response, objectMapper, HttpStatus.FORBIDDEN.value(), ErrorCode.FORBIDDEN)).and()
                .authorizeRequests()
                .antMatchers("/admin/api/auth/login", "/admin/api/auth/refresh",
                        "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                .anyRequest().authenticated();
        http.addFilterBefore(new AllowedHttpMethodFilter(objectMapper), ChannelProcessingFilter.class);
        http.addFilterBefore(new AdminJwtAuthenticationFilter(tokens, sessions, objectMapper),
                UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
