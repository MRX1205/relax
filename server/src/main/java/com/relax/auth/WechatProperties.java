package com.relax.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("relax.wechat")
public record WechatProperties(String appId, String appSecret) {

    public WechatProperties {
        appId = appId == null ? "" : appId;
        appSecret = appSecret == null ? "" : appSecret;
    }
}
