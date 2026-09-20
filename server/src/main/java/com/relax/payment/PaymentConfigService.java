package com.relax.payment;

import java.util.Optional;

import org.springframework.stereotype.Service;

@Service
public class PaymentConfigService {

    private final PaymentConfigMapper configMapper;

    PaymentConfigService(PaymentConfigMapper configMapper) {
        this.configMapper = configMapper;
    }

    public String getValue(String key) {
        return configMapper.findValue(key).orElse("");
    }

    public Optional<String> getOptionalValue(String key) {
        return configMapper.findValue(key);
    }

    public void updateValue(String key, String value) {
        configMapper.updateValue(key, value);
    }

    public boolean isEnabled() {
        return "true".equalsIgnoreCase(getValue("wxpay.enabled"));
    }

    public WxPayConfig getWxPayConfig() {
        return new WxPayConfig(
                getValue("wxpay.app-id"),
                getValue("wxpay.mch-id"),
                getValue("wxpay.api-key"),
                getValue("wxpay.serial-no"),
                getValue("wxpay.private-key"),
                getValue("wxpay.cert-path"),
                getValue("wxpay.notify-url"),
                getValue("wxpay.refund-notify-url")
        );
    }

    public record WxPayConfig(
            String appId,
            String mchId,
            String apiKey,
            String serialNo,
            String privateKey,
            String certPath,
            String notifyUrl,
            String refundNotifyUrl) {

        public boolean isValid() {
            return appId != null && !appId.isBlank()
                    && mchId != null && !mchId.isBlank()
                    && apiKey != null && !apiKey.isBlank()
                    && serialNo != null && !serialNo.isBlank()
                    && privateKey != null && !privateKey.isBlank();
        }
    }
}
