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

    public String getServicePhone() {
        String phone = settingMapper.getSetting("app.service.phone");
        return (phone != null && !phone.isBlank()) ? phone : "400-800-6688";
    }

    public String getCustomerQrUrl() {
        String qr = settingMapper.getSetting("app.customer.qr_url");
        return (qr != null && !qr.isBlank()) ? qr : "";
    }

    public String getInviteRewardAmount() {
        String amount = settingMapper.getSetting("app.invite.reward_amount");
        return (amount != null && !amount.isBlank()) ? amount : "30.00";
    }

    public PublicSettings getPublicSettings() {
        return new PublicSettings(getAppName(), isVipEnabled(), getServicePhone(), getCustomerQrUrl(), getInviteRewardAmount());
    }

    public void updateSettings(String appName, Boolean vipEnabled, String servicePhone, String customerQrUrl, String inviteRewardAmount) {
        if (appName != null && !appName.isBlank()) {
            settingMapper.upsertSetting("app.name", appName.trim(), "平台品牌对外展示名称");
        }
        if (vipEnabled != null) {
            settingMapper.upsertSetting("member.vip.enabled", String.valueOf(vipEnabled), "VIP会员入口开关: true-开启, false-关闭");
        }
        if (servicePhone != null && !servicePhone.isBlank()) {
            settingMapper.upsertSetting("app.service.phone", servicePhone.trim(), "平台统一客服电话");
        }
        if (customerQrUrl != null) {
            settingMapper.upsertSetting("app.customer.qr_url", customerQrUrl.trim(), "客服微信二维码图片URL");
        }
        if (inviteRewardAmount != null && !inviteRewardAmount.isBlank()) {
            settingMapper.upsertSetting("app.invite.reward_amount", inviteRewardAmount.trim(), "邀请新人活动奖励金额");
        }
    }

    public record PublicSettings(String appName, boolean vipEnabled, String servicePhone, String customerQrUrl, String inviteRewardAmount) {}
}
