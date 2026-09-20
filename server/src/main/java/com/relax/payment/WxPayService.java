package com.relax.payment;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.relax.common.api.BusinessException;

@Service
public class WxPayService {

    private static final Logger log = LoggerFactory.getLogger(WxPayService.class);
    private static final String UNIFIED_ORDER_URL = "https://api.mch.weixin.qq.com/v3/pay/transactions/jsapi";
    private static final String QUERY_ORDER_URL = "https://api.mch.weixin.qq.com/v3/pay/transactions/out-trade-no/%s?mchid=%s";
    private static final String CLOSE_ORDER_URL = "https://api.mch.weixin.qq.com/v3/pay/transactions/out-trade-no/%s/close";
    private static final String REFUND_URL = "https://api.mch.weixin.qq.com/v3/refund/domestic/refunds";

    private final PaymentConfigService configService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    WxPayService(PaymentConfigService configService, ObjectMapper objectMapper) {
        this.configService = configService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().build();
    }

    /**
     * Create a prepay_id for JSAPI payment
     */
    public String createPrepayId(String outTradeNo, BigDecimal amount, String description, String openid) {
        PaymentConfigService.WxPayConfig config = configService.getWxPayConfig();
        if (!config.isValid()) {
            throw new BusinessException("PAYMENT_CONFIG_INVALID", "微信支付配置不完整，请在管理后台配置");
        }

        try {
            long amountFen = amount.multiply(BigDecimal.valueOf(100)).longValue();
            String nonceStr = UUID.randomUUID().toString().replace("-", "");
            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);

            Map<String, Object> body = Map.of(
                    "appid", config.appId(),
                    "mchid", config.mchId(),
                    "description", description,
                    "out_trade_no", outTradeNo,
                    "notify_url", config.notifyUrl(),
                    "amount", Map.of("total", amountFen, "currency", "CNY"),
                    "payer", Map.of("openid", openid)
            );

            String bodyJson = objectMapper.writeValueAsString(body);
            String authorization = buildAuthorization("POST", "/v3/pay/transactions/jsapi", bodyJson, config);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(UNIFIED_ORDER_URL))
                    .header("Authorization", authorization)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode responseNode = objectMapper.readTree(response.body());

            if (response.statusCode() != 200) {
                String message = responseNode.has("message") ? responseNode.get("message").asText() : "预支付创建失败";
                log.error("WxPay unified order failed: {} - {}", response.statusCode(), response.body());
                throw new BusinessException("WXPAY_UNIFIED_ORDER_FAILED", message);
            }

            return responseNode.get("prepay_id").asText();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("WxPay createPrepayId error", e);
            throw new BusinessException("WXPAY_ERROR", "微信支付调用失败: " + e.getMessage());
        }
    }

    /**
     * Generate JSAPI payment parameters for frontend wx.requestPayment
     */
    public Map<String, String> generatePayParams(String prepayId, PaymentConfigService.WxPayConfig config) {
        String nonceStr = UUID.randomUUID().toString().replace("-", "");
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String signStr = config.appId() + "\n" + timestamp + "\n" + nonceStr + "\n" + "prepay_id=" + prepayId + "\n";

        try {
            String paySign = signWithRSA(signStr, config.privateKey());
            return Map.of(
                    "appId", config.appId(),
                    "timeStamp", timestamp,
                    "nonceStr", nonceStr,
                    "package", "prepay_id=" + prepayId,
                    "signType", "RSA",
                    "paySign", paySign
            );
        } catch (Exception e) {
            log.error("Generate pay params error", e);
            throw new BusinessException("WXPAY_SIGN_ERROR", "支付参数签名失败");
        }
    }

    /**
     * Query order status
     */
    public String queryOrder(String outTradeNo) {
        PaymentConfigService.WxPayConfig config = configService.getWxPayConfig();
        try {
            String path = String.format("/v3/pay/transactions/out-trade-no/%s?mchid=%s", outTradeNo, config.mchId());
            String url = String.format(QUERY_ORDER_URL, outTradeNo, config.mchId());
            String authorization = buildAuthorization("GET", path, "", config);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", authorization)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode responseNode = objectMapper.readTree(response.body());

            if (response.statusCode() == 200) {
                return responseNode.has("trade_state") ? responseNode.get("trade_state").asText() : "UNKNOWN";
            }
            return "QUERY_FAILED";
        } catch (Exception e) {
            log.error("WxPay queryOrder error", e);
            return "QUERY_ERROR";
        }
    }

    /**
     * Verify payment notification signature
     */
    public boolean verifyNotification(String timestamp, String nonce, String body, String signature, String serialNo) {
        PaymentConfigService.WxPayConfig config = configService.getWxPayConfig();
        try {
            String verifyStr = timestamp + "\n" + nonce + "\n" + body + "\n";
            // In production, fetch WeChat platform certificate by serialNo and verify
            // For now, return true in development mode
            if (!configService.isEnabled()) {
                return true;
            }
            // TODO: Implement proper signature verification with platform certificate
            return true;
        } catch (Exception e) {
            log.error("Verify notification signature error", e);
            return false;
        }
    }

    private String buildAuthorization(String method, String path, String body, PaymentConfigService.WxPayConfig config) {
        try {
            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
            String nonceStr = UUID.randomUUID().toString().replace("-", "");
            String message = method + "\n" + path + "\n" + timestamp + "\n" + nonceStr + "\n" + body + "\n";
            String signature = signWithRSA(message, config.privateKey());

            return "WECHATPAY2-SHA256-RSA2048 "
                    + "mchid=\"" + config.mchId() + "\","
                    + "nonce_str=\"" + nonceStr + "\","
                    + "timestamp=\"" + timestamp + "\","
                    + "serial_no=\"" + config.serialNo() + "\","
                    + "signature=\"" + signature + "\"";
        } catch (Exception e) {
            throw new BusinessException("WXPAY_AUTH_ERROR", "构建支付认证头失败");
        }
    }

    private String signWithRSA(String message, String privateKeyPem) throws Exception {
        String cleanKey = privateKeyPem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] keyBytes = Base64.getDecoder().decode(cleanKey);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(message.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature.sign());
    }
}
