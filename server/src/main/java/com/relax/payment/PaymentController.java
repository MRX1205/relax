package com.relax.payment;

import java.math.BigDecimal;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.relax.auth.CurrentUser;
import com.relax.common.api.ApiResponse;

@RestController
@RequestMapping("/api/v1")
public class PaymentController {

    private final PaymentService paymentService;
    private final MockPaymentGateway mockGateway;

    PaymentController(PaymentService paymentService, MockPaymentGateway mockGateway) {
        this.paymentService = paymentService;
        this.mockGateway = mockGateway;
    }

    @PostMapping("/orders/{orderNo}/payments")
    ApiResponse<PaymentMapper.PaymentView> createPayment(@AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable String orderNo) {
        return ApiResponse.success(paymentService.createPayment(currentUser.id(), orderNo));
    }

    @GetMapping("/payments/{paymentNo}")
    ApiResponse<PaymentMapper.PaymentView> getPayment(@PathVariable String paymentNo) {
        return ApiResponse.success(paymentService.getPayment(paymentNo));
    }

    @PostMapping("/payments/{paymentNo}/simulate")
    ApiResponse<MockPaymentGateway.MockPayResult> simulatePayment(@PathVariable String paymentNo,
            @RequestBody(required = false) SimulateRequest request) {
        String scenario = (request != null && request.scenario() != null) ? request.scenario() : "SUCCESS";
        MockPaymentGateway.MockPayResult result;
        if ("SUCCESS".equals(scenario)) {
            result = mockGateway.simulatePaySuccess(paymentNo);
            // 如果模拟支付成功，更新订单状态
            if ("SUCCESS".equals(result.code())) {
                paymentService.processMockPayment(paymentNo);
            }
        } else {
            result = mockGateway.simulatePayFail(paymentNo);
        }
        return ApiResponse.success(result);
    }

    @PostMapping("/payments/notify")
    ApiResponse<PaymentService.PaymentResult> handleNotify(@RequestBody PaymentService.PaymentNotifyRequest request) {
        return ApiResponse.success(paymentService.handleNotify(request));
    }

    public record SimulateRequest(String scenario) {}
}
