package com.relax.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
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
    private final WxPayService wxPayService;
    private final PaymentConfigService configService;
    private final SettlementService settlementService;

    PaymentService(PaymentMapper paymentMapper, OrderMapper orderMapper, OrderService orderService,
            MockPaymentGateway mockGateway, WxPayService wxPayService,
            PaymentConfigService configService, SettlementService settlementService) {
        this.paymentMapper = paymentMapper;
        this.orderMapper = orderMapper;
        this.orderService = orderService;
        this.mockGateway = mockGateway;
        this.wxPayService = wxPayService;
        this.configService = configService;
        this.settlementService = settlementService;
    }

    /**
     * Create a payment for an order.
     * Returns a PaymentResultView with channel=MOCK or channel=WXPAY.
     * When WXPAY, payParams will contain the wx.requestPayment arguments.
     */
    @Transactional
    public PaymentResultView createPayment(long userId, String orderNo, String openid) {
        OrderService.OrderDetailView detail = orderService.getOrderDetail(orderNo);
        if (detail.order().userId() != userId) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ORDER_NOT_OWNED", "无权操作该订单");
        }
        if (!"PENDING_PAYMENT".equals(detail.order().status())) {
            throw new BusinessException("ORDER_NOT_PAYABLE", "当前订单状态不可支付");
        }

        // Check if a pending payment already exists
        Optional<PaymentMapper.PaymentView> existing = paymentMapper.findPendingByOrderId(detail.order().id());
        if (existing.isPresent()) {
            PaymentMapper.PaymentView existingPay = existing.get();
            if (configService.isEnabled()) {
                return enrichWithWxPayParams(existingPay, openid, detail.amount().payableAmount());
            }
            return PaymentResultView.from(existingPay, null);
        }

        boolean useRealPay = configService.isEnabled();
        String channel = useRealPay ? "WXPAY" : "MOCK";

        PaymentMapper.PaymentView payment = createNewPayment(detail.order().id(), detail.amount().payableAmount(), channel);

        if (useRealPay) {
            return enrichWithWxPayParams(payment, openid, detail.amount().payableAmount());
        } else {
            // Register with mock gateway
            mockGateway.registerPayment(payment.paymentNo(), orderNo, detail.amount().payableAmount());
            return PaymentResultView.from(payment, null);
        }
    }

    private PaymentResultView enrichWithWxPayParams(
            PaymentMapper.PaymentView payment, String openid, BigDecimal amount) {
        PaymentConfigService.WxPayConfig config = configService.getWxPayConfig();
        if (!config.isValid()) {
            throw new BusinessException("PAYMENT_CONFIG_INVALID", "微信支付配置不完整，请在管理后台配置");
        }
        try {
            String description = "东莞到家服务";
            String prepayId = wxPayService.createPrepayId(payment.paymentNo(), amount, description, openid);
            Map<String, String> params = wxPayService.generatePayParams(prepayId, config);
            WxPayParams wxPayParams = new WxPayParams(
                    params.get("timeStamp"),
                    params.get("nonceStr"),
                    params.get("package"),
                    "RSA",
                    params.get("paySign")
            );
            return PaymentResultView.from(payment, wxPayParams);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create WxPay prepay", e);
            throw new BusinessException("WXPAY_ERROR", "微信支付初始化失败: " + e.getMessage());
        }
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

    public PaymentResultView getPayment(String paymentNo) {
        PaymentMapper.PaymentView view = paymentMapper.findByPaymentNo(paymentNo)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "支付单不存在"));
        return PaymentResultView.from(view, null);
    }

    private PaymentMapper.PaymentView createNewPayment(long orderId, BigDecimal amount, String channel) {
        long id = IdWorker.getId();
        String paymentNo = "PAY" + System.currentTimeMillis() + String.format("%04d", (int)(Math.random() * 10000));
        LocalDateTime expireAt = LocalDateTime.now().plusMinutes(PAYMENT_TIMEOUT_MINUTES);
        paymentMapper.insert(id, paymentNo, orderId, channel, amount, expireAt);
        return paymentMapper.findByPaymentNo(paymentNo).orElseThrow();
    }

    public record WxPayParams(String timeStamp, String nonceStr, String packageStr, String signType, String paySign) {}

    public record PaymentResultView(long id, String paymentNo, long orderId, String channel, BigDecimal amount,
            String status, String transactionId, LocalDateTime expireAt, LocalDateTime paidAt,
            LocalDateTime createdAt, WxPayParams payParams) {
        public static PaymentResultView from(PaymentMapper.PaymentView v, WxPayParams p) {
            return new PaymentResultView(v.id(), v.paymentNo(), v.orderId(), v.channel(), v.amount(),
                    v.status(), v.transactionId(), v.expireAt(), v.paidAt(), v.createdAt(), p);
        }
    }

    public record PaymentNotifyRequest(String paymentNo, String transactionId, BigDecimal amount, String result) {}

    public record PaymentResult(String code, String message) {}
}
