package com.relax.auth;

import com.relax.common.api.BusinessException;

import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Profile({"staging", "production"})
class WechatApiGateway implements WechatGateway {

    private static final String API_BASE_URL = "https://api.weixin.qq.com";

    private final WechatProperties properties;
    private final RestClient restClient;

    WechatApiGateway(WechatProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.baseUrl(API_BASE_URL).build();
    }

    @Override
    public WechatIdentity exchangeLoginCode(String code) {
        requireCredentials();
        SessionResponse response = restClient.get()
                .uri(uri -> uri.path("/sns/jscode2session")
                        .queryParam("appid", properties.appId())
                        .queryParam("secret", properties.appSecret())
                        .queryParam("js_code", code)
                        .queryParam("grant_type", "authorization_code")
                        .build())
                .retrieve()
                .body(SessionResponse.class);
        if (response == null || response.openid() == null || response.errcode() != null) {
            throw new BusinessException("WECHAT_LOGIN_FAILED", "微信登录失败，请稍后重试");
        }
        return new WechatIdentity(response.openid(), response.unionid());
    }

    @Override
    public String exchangePhoneCode(String code) {
        requireCredentials();
        AccessTokenResponse token = restClient.post()
                .uri("/cgi-bin/stable_token")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AccessTokenRequest("client_credential", properties.appId(), properties.appSecret(), false))
                .retrieve()
                .body(AccessTokenResponse.class);
        if (token == null || token.access_token() == null) {
            throw new BusinessException("WECHAT_ACCESS_TOKEN_FAILED", "微信接口暂时不可用");
        }

        PhoneResponse response = restClient.post()
                .uri(uri -> uri.path("/wxa/business/getuserphonenumber")
                        .queryParam("access_token", token.access_token())
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new PhoneRequest(code))
                .retrieve()
                .body(PhoneResponse.class);
        if (response == null || response.errcode() != 0 || response.phone_info() == null) {
            throw new BusinessException("WECHAT_PHONE_CODE_INVALID", "手机号授权凭证无效");
        }
        return response.phone_info().purePhoneNumber();
    }

    private void requireCredentials() {
        if (properties.appId().isBlank() || properties.appSecret().isBlank()) {
            throw new IllegalStateException("WECHAT_APP_ID and WECHAT_APP_SECRET must be configured");
        }
    }

    private record SessionResponse(String openid, String unionid, Integer errcode) {
    }

    private record AccessTokenRequest(
            String grant_type,
            String appid,
            String secret,
            boolean force_refresh) {
    }

    private record AccessTokenResponse(String access_token) {
    }

    private record PhoneRequest(String code) {
    }

    private record PhoneResponse(int errcode, PhoneInfo phone_info) {
    }

    private record PhoneInfo(String purePhoneNumber) {
    }
}
