package com.relax.config;

import com.relax.auth.AccessTokenFilter;
import com.relax.auth.ApiSecurityErrorWriter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AccessTokenFilter accessTokenFilter,
            ApiSecurityErrorWriter errorWriter) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> errorWriter.write(
                                response, HttpStatus.UNAUTHORIZED.value(), "AUTHENTICATION_REQUIRED", "请先登录"))
                        .accessDeniedHandler((request, response, exception) -> errorWriter.write(
                                response, HttpStatus.FORBIDDEN.value(), "ACCESS_DENIED", "当前账号无权执行该操作")))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/api/v1/health",
                                "/api/v1/auth/wechat-login",
                                "/api/v1/auth/phone-login",
                                "/api/v1/auth/password-login",
                                "/api/v1/auth/role-wechat-login",
                                "/actuator/health",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        // Public file serving (images, etc.)
                        .requestMatchers(HttpMethod.GET, "/api/v1/public/**").permitAll()
                        // Public API endpoints
                        .requestMatchers(HttpMethod.GET, "/api/v1/agreements/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/regions/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/home").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/projects").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/projects/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/technicians").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/technicians/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/technicians/*/schedules").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/banners").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/payment/mode").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/system/settings").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/search").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/technicians/*/reviews").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/reviews").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/coupons/available").permitAll()
                        // File upload requires auth but uses special token
                        .requestMatchers(HttpMethod.PUT, "/api/v1/files/*/content").permitAll()
                        // WeChat payment callback (verified by signature)
                        .requestMatchers("/api/v1/payments/wechat/**").permitAll()
                        .requestMatchers("/api/v1/refunds/wechat/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(accessTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
