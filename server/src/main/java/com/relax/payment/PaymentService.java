package com.relax.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.relax.common.api.BusinessException;
import com.relax.order.OrderMapper;
import com.relax.order.OrderService;
import com.relax.settlement.SettlementService;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final int PAYMENT_TIMEOUT_MINUTES = 15;

    private final PaymentMapper paymentMapper;
    private final OrderMapper orderMapper;
    private final OrderService orderService;
    private final MockPaymentGateway mockGateway;
    private final SettlementService settlementService;

    PaymentService(PaymentMapper paymentMapper, OrderMapper orderMapper, OrderService orderService,
            MockPaymentGateway mockGateway, SettlementService settlementService) {
        this.paymentMapper = paymentMapper;
        this.orderMapper = orderMapper;
        this.orderService = orderService;
        this.mockGateway = mockGateway;
        this.settlementService = settlementService;
    }

    @Transactional
    public PaymentMapper.PaymentView createPayment(long userId, String orderNo) {
        OrderService.OrderDetailView detail = orderService.getOrderDetail(orderNo);
        if (detail.order().userId() != userId) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ORDER_NOT_OWNED", "无权操作该订单");
        }
        if (!"PENDING_PAYMENT".equals(detail.order().status())) {
            throw new BusinessException("ORDER_NOT_PAYABLE", "当前订单状态不可支付");
        }
        Optional<PaymentMapper.PaymentView> existing = paymentMapper.findPendingByOrderId(detail.order().id());
        if (existing.isPresent()) {
            return existing.get();
        }
        PaymentMapper.PaymentView payment = createNewPayment(detail.order().id(), detail.amount().payableAmount(), "MOCK");
        // 注册到模拟支付网关
        mockGateway.registerPayment(payment.paymentNo(), orderNo, detail.amount().payableAmount());
        return payment;
    }

    @Transactional
    public void processMockPayment(String paymentNo) {
        PaymentMapper.PaymentView payment = paymentMapper.findByPaymentNo(paymentNo)
                .orElseThrow(() -> new BusinessException("PAYMENT_NOT_FOUND", "支付单不存在"));
        if (!"PENDING".equals(payment.status())) {
            return;
        }
        String transactionId = "MOCK_" + System.currentTimeMillis();
        if (paymentMapper.markSuccess(paymentNo, transactionId) > 0) {
            // 通过orderId获取orderNo
            OrderMapper.OrderView order = orderMapper.findByOrderId(payment.orderId());
            if (order != null) {
                orderService.markPaid(order.orderNo(), transactionId, payment.amount());
            }
        }
    }

    @Transactional
    public PaymentResult handleNotify(PaymentNotifyRequest request) {
        if (paymentMapper.countDuplicateNotify(request.paymentNo(), request.transactionId()) > 0) {
            return new PaymentResult("ALREADY_PROCESSED", "已处理");
        }

        PaymentMapper.PaymentView payment = paymentMapper.findByPaymentNo(request.paymentNo())
                .orElseThrow(() -> new BusinessException("PAYMENT_NOT_FOUND", "支付单不存在"));

        if (payment.amount().compareTo(request.amount()) != 0) {
            long notifyId = IdWorker.getId();
            paymentMapper.insertNotify(notifyId, request.paymentNo(), request.transactionId(), "AMOUNT_MISMATCH");
            throw new BusinessException("PAYMENT_AMOUNT_MISMATCH", "支付金额不一致");
        }

        if ("SUCCESS".equals(request.result())) {
            if (paymentMapper.markSuccess(request.paymentNo(), request.transactionId()) > 0) {
                OrderService.OrderDetailView detail = orderService.getOrderDetail(
                        String.valueOf(payment.orderId()));
                orderService.markPaid(detail.order().orderNo(), request.transactionId(), request.amount());
            }
        } else {
            paymentMapper.markFailed(request.paymentNo());
        }

        long notifyId = IdWorker.getId();
        paymentMapper.insertNotify(notifyId, request.paymentNo(), request.transactionId(), "PROCESSED");
        return new PaymentResult("OK", "处理成功");
    }

    public PaymentMapper.PaymentView getPayment(String paymentNo) {
        return paymentMapper.findByPaymentNo(paymentNo)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "支付单不存在"));
    }

    private PaymentMapper.PaymentView createNewPayment(long orderId, BigDecimal amount, String channel) {
        long id = IdWorker.getId();
        String paymentNo = "PAY" + System.currentTimeMillis() + String.format("%04d", (int)(Math.random() * 10000));
        LocalDateTime expireAt = LocalDateTime.now().plusMinutes(PAYMENT_TIMEOUT_MINUTES);
        paymentMapper.insert(id, paymentNo, orderId, channel, amount, expireAt);
        return paymentMapper.findByPaymentNo(paymentNo).orElseThrow();
    }

    public record PaymentNotifyRequest(String paymentNo, String transactionId, BigDecimal amount, String result) {}

    public record PaymentResult(String code, String message) {}
}
