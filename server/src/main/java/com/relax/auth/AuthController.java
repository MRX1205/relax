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
import com.relax.common.api.BusinessException;

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

    @PostMapping("/auth/phone-login")
    ApiResponse<AuthService.LoginResult> phoneLogin(@Valid @RequestBody PhoneLoginRequest request) {
        return ApiResponse.success(authService.loginByPhone(request.phone()));
    }

    @PostMapping("/auth/password-login")
    ApiResponse<AuthService.LoginResult> passwordLogin(@Valid @RequestBody PasswordLoginRequest request) {
        return ApiResponse.success(authService.loginByPassword(request.phone(), request.password(), request.targetRole()));
    }

    @PostMapping("/auth/role-wechat-login")
    ApiResponse<AuthService.LoginResult> roleWechatLogin(@Valid @RequestBody RoleWechatLoginRequest request) {
        return ApiResponse.success(authService.loginByRoleWechat(request.code(), request.targetRole()));
    }

    @PostMapping("/auth/bind-wechat")
    ApiResponse<AuthService.AccountView> bindWechat(@AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody WechatRequest request) {
        return ApiResponse.success(authService.bindWechat(currentUser.id(), request.code()));
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

    @PostMapping("/auth/verify-admin-pin")
    ApiResponse<Boolean> verifyAdminPin(@AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody AdminPinRequest request) {
        AuthService.AccountView account = authService.account(currentUser.id());
        if (!account.roles().contains("ADMIN") && !account.roles().contains("SUPER_ADMIN")) {
            throw new BusinessException(org.springframework.http.HttpStatus.FORBIDDEN, "FORBIDDEN", "当前账号未被授权管理员身份");
        }
        if (!"888888".equals(request.pin())) {
            throw new BusinessException(org.springframework.http.HttpStatus.BAD_REQUEST, "PIN_INVALID", "管理员安全口令错误");
        }
        return ApiResponse.success(true);
    }

    public record LoginRequest(@NotBlank @Size(max = 200) String code) {
    }

    public record PhoneLoginRequest(@NotBlank @Pattern(regexp = "1\\d{10}") String phone) {
    }

    public record PasswordLoginRequest(
            @NotBlank @Pattern(regexp = "1\\d{10}") String phone,
            @NotBlank @Size(max = 64) String password,
            @NotBlank @Pattern(regexp = "USER|TECHNICIAN|ADMIN|SUPER_ADMIN") String targetRole) {
    }

    public record RoleWechatLoginRequest(
            @NotBlank @Size(max = 200) String code,
            @NotBlank @Pattern(regexp = "USER|TECHNICIAN|ADMIN|SUPER_ADMIN") String targetRole) {
    }

    public record WechatRequest(@NotBlank @Size(max = 200) String code) {
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

    public record AdminPinRequest(@NotBlank @Size(max = 20) String pin) {
    }
}
