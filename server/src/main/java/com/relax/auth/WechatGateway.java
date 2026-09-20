package com.relax.auth;

public interface WechatGateway {

    WechatIdentity exchangeLoginCode(String code);

    String exchangePhoneCode(String code);

    record WechatIdentity(String openId, String unionId) {
    }
}
