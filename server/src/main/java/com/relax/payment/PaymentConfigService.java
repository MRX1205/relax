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
        int updated = configMapper.updateValue(key, value);
        if (updated == 0) {
            configMapper.upsertValue(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId(), key, value);
        }
    }

    public String getPaymentMode() {
        String mode = getValue("payment.mode");
        if (mode != null && !mode.isBlank()) {
            return mode.toUpperCase();
        }
        return isEnabled() ? "WXPAY" : "MOCK";
    }

    public void setPaymentMode(String mode) {
        String cleanMode = (mode != null) ? mode.trim().toUpperCase() : "MOCK";
        if (!cleanMode.equals("MOCK") && !cleanMode.equals("WXPAY") && !cleanMode.equals("OFFLINE")) {
            cleanMode = "MOCK";
        }
        updateValue("payment.mode", cleanMode);
        updateValue("wxpay.enabled", cleanMode.equals("WXPAY") ? "true" : "false");
    }

    public boolean isEnabled() {
        String mode = getValue("payment.mode");
        if ("WXPAY".equalsIgnoreCase(mode)) {
            return true;
        }
        if ("MOCK".equalsIgnoreCase(mode) || "OFFLINE".equalsIgnoreCase(mode)) {
            return false;
        }
        return "true".equalsIgnoreCase(getValue("wxpay.enabled"));
    }

    public java.util.Map<String, String> getAllConfigs() {
        java.util.Map<String, String> result = new java.util.HashMap<>();
        for (java.util.Map<String, String> row : configMapper.findAll()) {
            result.put(row.get("config_key"), row.get("config_value"));
        }
        // Normalize keys for frontend expectations
        result.put("payment.mode", getPaymentMode());
        result.put("wxpay.enabled", String.valueOf(isEnabled()));
        return result;
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
