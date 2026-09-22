package com.relax.system;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.relax.common.api.ApiResponse;

@RestController
@RequestMapping("/api/v1")
public class SystemSettingController {

    private final SystemSettingService settingService;

    public SystemSettingController(SystemSettingService settingService) {
        this.settingService = settingService;
    }

    @GetMapping("/system/settings")
    public ApiResponse<SystemSettingService.PublicSettings> getPublicSettings() {
        return ApiResponse.success(settingService.getPublicSettings());
    }

    @PutMapping("/admin/system/settings")
    @PreAuthorize("hasAuthority('admin:manage') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ApiResponse<SystemSettingService.PublicSettings> updateSettings(@RequestBody UpdateSettingsRequest request) {
        settingService.updateSettings(request.appName(), request.vipEnabled());
        return ApiResponse.success(settingService.getPublicSettings());
    }

    public record UpdateSettingsRequest(String appName, Boolean vipEnabled) {}
}
