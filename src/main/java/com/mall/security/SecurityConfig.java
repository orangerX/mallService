package com.mall.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.auth.session.AuthSessionService;
import com.mall.common.api.ErrorCode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.channel.ChannelProcessingFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtTokenService tokenService;
    private final AuthSessionService sessionService;
    private final ObjectMapper objectMapper;

    public SecurityConfig(CustomUserDetailsService userDetailsService, JwtTokenService tokenService,
                          AuthSessionService sessionService, ObjectMapper objectMapper) {
        this.userDetailsService = userDetailsService;
        this.tokenService = tokenService;
        this.sessionService = sessionService;
        this.objectMapper = objectMapper;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        AllowedHttpMethodFilter methodFilter = new AllowedHttpMethodFilter(objectMapper);
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(tokenService, sessionService, objectMapper);

        http.cors().disable()
                .csrf().disable()
                .formLogin().disable()
                .httpBasic().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .exceptionHandling()
                .authenticationEntryPoint((request, response, exception) -> JsonSecurityResponseWriter.write(
                        response, objectMapper, HttpStatus.UNAUTHORIZED.value(), ErrorCode.UNAUTHORIZED
                ))
                .accessDeniedHandler((request, response, exception) -> JsonSecurityResponseWriter.write(
                        response, objectMapper, HttpStatus.FORBIDDEN.value(), ErrorCode.FORBIDDEN
                ))
                .and()
                .authorizeRequests()
                .antMatchers(
                        "/api/auth/register",
                        "/api/auth/login",
                        "/api/auth/refresh",
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**"
                ).permitAll()
                .anyRequest().authenticated();

        http.addFilterBefore(methodFilter, ChannelProcessingFilter.class);
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
