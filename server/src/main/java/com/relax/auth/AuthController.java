package com.relax.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.relax.common.api.ApiResponse;

@RestController
@RequestMapping("/api/v1")
public class AuthController {

    private final AuthService authService;

    AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth/wechat-login")
    ApiResponse<AuthService.LoginResult> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request.code()));
    }

    @PostMapping("/auth/logout")
    ApiResponse<Void> logout(@RequestHeader("Authorization") String authorization) {
        authService.logout(authorization.substring("Bearer ".length()));
        return ApiResponse.success(null);
    }

    @GetMapping("/me")
    ApiResponse<AuthService.AccountView> me(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.success(authService.account(currentUser.id()));
    }

    @PostMapping("/auth/bind-phone")
    ApiResponse<AuthService.AccountView> bindPhone(@AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody PhoneRequest request) {
        return ApiResponse.success(authService.bindPhone(currentUser.id(), request.code()));
    }

    @PutMapping("/me/profile")
    ApiResponse<AuthService.AccountView> updateProfile(@AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody ProfileRequest request) {
        return ApiResponse.success(authService.updateProfile(currentUser.id(), request.nickname(), request.avatarUrl()));
    }

    @PutMapping("/me/last-role")
    ApiResponse<AuthService.AccountView> switchRole(@AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody RoleRequest request) {
        return ApiResponse.success(authService.switchRole(currentUser.id(), request.role()));
    }

    public record LoginRequest(@NotBlank @Size(max = 200) String code) {
    }

    public record PhoneRequest(@NotBlank @Size(max = 200) String code) {
    }

    public record ProfileRequest(
            @NotBlank @Size(max = 64) String nickname,
            @Size(max = 500) String avatarUrl) {
    }

    public record RoleRequest(
            @NotBlank @Pattern(regexp = "USER|TECHNICIAN|ADMIN|SUPER_ADMIN") String role) {
    }
}
