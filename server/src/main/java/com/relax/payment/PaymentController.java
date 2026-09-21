package com.relax.payment;

import java.math.BigDecimal;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.relax.auth.CurrentUser;
import com.relax.common.api.ApiResponse;
import com.relax.common.api.BusinessException;

@RestController
@RequestMapping("/api/v1")
public class PaymentController {

    private final PaymentService paymentService;
    private final MockPaymentGateway mockGateway;
    private final WxPayService wxPayService;
    private final PaymentConfigService configService;

    PaymentController(PaymentService paymentService, MockPaymentGateway mockGateway,
            WxPayService wxPayService, PaymentConfigService configService) {
        this.paymentService = paymentService;
        this.mockGateway = mockGateway;
        this.wxPayService = wxPayService;
        this.configService = configService;
    }

    /**
     * Create a payment for an order.
     * When wxpay.enabled=false → Mock payment (no openid needed).
     * When wxpay.enabled=true → Real WeChat Pay, requires user's openid from request.
     */
    @PostMapping("/orders/{orderNo}/payments")
    ApiResponse<PaymentService.PaymentResultView> createPayment(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable String orderNo,
            @RequestBody(required = false) CreatePaymentRequest body) {
        // openid is passed from frontend (stored after wechat login) for JSAPI payment
        String openid = (body != null && body.openid() != null) ? body.openid()
                : currentUser.openId();
        return ApiResponse.success(paymentService.createPayment(currentUser.id(), orderNo, openid));
    }

    @GetMapping("/payments/{paymentNo}")
    ApiResponse<PaymentService.PaymentResultView> getPayment(@PathVariable String paymentNo) {
        return ApiResponse.success(paymentService.getPayment(paymentNo));
    }

    /**
     * Simulate payment (only works when wxpay.enabled=false / Mock mode).
     */
    @PostMapping("/payments/{paymentNo}/simulate")
    ApiResponse<MockPaymentGateway.MockPayResult> simulatePayment(
            @PathVariable String paymentNo,
            @RequestBody(required = false) SimulateRequest request) {
        if (configService.isEnabled()) {
            throw new BusinessException("MOCK_DISABLED", "当前为真实支付模式，无法模拟支付");
        }
        String scenario = (request != null && request.scenario() != null) ? request.scenario() : "SUCCESS";
        MockPaymentGateway.MockPayResult result;
        if ("SUCCESS".equals(scenario)) {
            result = mockGateway.simulatePaySuccess(paymentNo);
            if ("SUCCESS".equals(result.code())) {
                paymentService.processMockPayment(paymentNo);
            }
        } else {
            result = mockGateway.simulatePayFail(paymentNo);
        }
        return ApiResponse.success(result);
    }

    /**
     * WeChat Pay payment notification callback.
     */
    @PostMapping("/payments/wechat/notify")
    ApiResponse<PaymentService.PaymentResult> handleWxPayNotify(
            @RequestBody String body,
            HttpServletRequest httpRequest) {
        // Verify WeChat signature
        String timestamp = httpRequest.getHeader("Wechatpay-Timestamp");
        String nonce = httpRequest.getHeader("Wechatpay-Nonce");
        String signature = httpRequest.getHeader("Wechatpay-Signature");
        String serial = httpRequest.getHeader("Wechatpay-Serial");

        if (!wxPayService.verifyNotification(timestamp, nonce, body, signature, serial)) {
            throw new BusinessException("SIGNATURE_INVALID", "微信支付签名验证失败");
        }

        // Parse and process the notification
        // (Real implementation would decrypt the resource body and extract payment details)
        return ApiResponse.success(new PaymentService.PaymentResult("OK", "处理成功"));
    }

    /**
     * Internal/test notification endpoint for testing notify flow.
     */
    @PostMapping("/payments/notify")
    ApiResponse<PaymentService.PaymentResult> handleNotify(
            @RequestBody PaymentService.PaymentNotifyRequest request) {
        return ApiResponse.success(paymentService.handleNotify(request));
    }

    public record CreatePaymentRequest(String openid) {}

    public record SimulateRequest(String scenario) {}
}
