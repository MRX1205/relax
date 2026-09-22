package com.relax.system;

import org.springframework.stereotype.Service;

@Service
public class SystemSettingService {

    private final SystemSettingMapper settingMapper;

    public SystemSettingService(SystemSettingMapper settingMapper) {
        this.settingMapper = settingMapper;
    }

    public String getAppName() {
        String name = settingMapper.getSetting("app.name");
        return (name != null && !name.isBlank()) ? name : "东莞到家";
    }

    public boolean isVipEnabled() {
        String val = settingMapper.getSetting("member.vip.enabled");
        return "true".equalsIgnoreCase(val);
    }

    public PublicSettings getPublicSettings() {
        return new PublicSettings(getAppName(), isVipEnabled());
    }

    public void updateSettings(String appName, Boolean vipEnabled) {
        if (appName != null && !appName.isBlank()) {
            settingMapper.upsertSetting("app.name", appName.trim(), "平台品牌对外展示名称");
        }
        if (vipEnabled != null) {
            settingMapper.upsertSetting("member.vip.enabled", String.valueOf(vipEnabled), "VIP会员入口开关: true-开启, false-关闭");
        }
    }

    public record PublicSettings(String appName, boolean vipEnabled) {}
}
