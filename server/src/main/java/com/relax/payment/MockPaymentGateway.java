package com.relax.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 模拟支付网关，用于开发测试
 */
@Component
public class MockPaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(MockPaymentGateway.class);
    private final Map<String, MockPayment> payments = new ConcurrentHashMap<>();

    /**
     * 注册支付单到模拟网关
     */
    public void registerPayment(String paymentNo, String orderNo, BigDecimal amount) {
        log.info("Registering payment: {}, order: {}, amount: {}", paymentNo, orderNo, amount);
        MockPayment payment = new MockPayment(paymentNo, orderNo, amount, "按摩服务", "PENDING", LocalDateTime.now());
        payments.put(paymentNo, payment);
        log.info("Payment registered. Total payments: {}", payments.size());
    }

    /**
     * 模拟支付成功
     */
    public MockPayResult simulatePaySuccess(String paymentNo) {
        log.info("Simulating payment success: {}", paymentNo);
        log.info("Available payments: {}", payments.keySet());
        MockPayment payment = payments.get(paymentNo);
        if (payment == null) {
            log.error("Payment not found: {}", paymentNo);
            return new MockPayResult("FAIL", "支付单不存在", null);
        }
        if (!"PENDING".equals(payment.getStatus())) {
            return new MockPayResult("FAIL", "支付单状态不正确", null);
        }
        
        String transactionId = "TXN_" + System.currentTimeMillis();
        payment.setStatus("SUCCESS");
        payment.setTransactionId(transactionId);
        payment.setPaidAt(LocalDateTime.now());
        
        return new MockPayResult("SUCCESS", "支付成功", transactionId);
    }

    /**
     * 模拟支付失败
     */
    public MockPayResult simulatePayFail(String paymentNo) {
        MockPayment payment = payments.get(paymentNo);
        if (payment == null) {
            return new MockPayResult("FAIL", "支付单不存在", null);
        }
        if (!"PENDING".equals(payment.getStatus())) {
            return new MockPayResult("FAIL", "支付单状态不正确", null);
        }
        
        payment.setStatus("FAILED");
        payment.setFailReason("用户取消支付");
        
        return new MockPayResult("FAIL", "支付失败", null);
    }

    /**
     * 查询支付状态
     */
    public MockPayment queryPayment(String paymentNo) {
        return payments.get(paymentNo);
    }

    public record MockPayResult(String code, String message, String transactionId) {}

    public static class MockPayment {
        private String paymentNo;
        private String orderNo;
        private BigDecimal amount;
        private String description;
        private String status;
        private String transactionId;
        private String failReason;
        private BigDecimal refundedAmount = BigDecimal.ZERO;
        private LocalDateTime createdAt;
        private LocalDateTime paidAt;

        public MockPayment(String paymentNo, String orderNo, BigDecimal amount, String description, 
                          String status, LocalDateTime createdAt) {
            this.paymentNo = paymentNo;
            this.orderNo = orderNo;
            this.amount = amount;
            this.description = description;
            this.status = status;
            this.createdAt = createdAt;
        }

        // Getters and Setters
        public String getPaymentNo() { return paymentNo; }
        public void setPaymentNo(String paymentNo) { this.paymentNo = paymentNo; }
        public String getOrderNo() { return orderNo; }
        public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
        public String getFailReason() { return failReason; }
        public void setFailReason(String failReason) { this.failReason = failReason; }
        public BigDecimal getRefundedAmount() { return refundedAmount; }
        public void setRefundedAmount(BigDecimal refundedAmount) { this.refundedAmount = refundedAmount; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getPaidAt() { return paidAt; }
        public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    }
}
