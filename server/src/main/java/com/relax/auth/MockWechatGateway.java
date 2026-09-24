package com.relax.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.relax.common.api.BusinessException;

@Component
@Profile({"local", "test", "dev"})
class MockWechatGateway implements WechatGateway {

    @Override
    public WechatIdentity exchangeLoginCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException("WECHAT_CODE_INVALID", "微信登录凭证无效");
        }
        return new WechatIdentity("mock:" + sha256(code).substring(0, 32), null);
    }

    public WechatIdentity exchangeLoginCodeWithDevice(String code, String deviceId) {
        if (deviceId != null && !deviceId.isBlank()) {
            return new WechatIdentity("mock:device:" + sha256(deviceId.trim()).substring(0, 24), null);
        }
        return exchangeLoginCode(code);
    }

    @Override
    public String exchangePhoneCode(String code) {
        if (code != null && code.startsWith("mock-phone-")) {
            return code.substring(11);
        }
        if (code != null && code.matches("1\\d{10}")) {
            return code;
        }
        if (code != null && !code.isBlank()) {
            long hash = Math.abs((long) code.hashCode());
            return String.format("139%08d", hash % 100000000L);
        }
        throw new BusinessException("WECHAT_PHONE_CODE_INVALID", "手机号授权凭证无效");
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
