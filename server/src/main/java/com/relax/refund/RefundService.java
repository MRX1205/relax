package com.relax.refund;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.relax.common.api.BusinessException;
import com.relax.notification.NotificationService;
import com.relax.order.OrderMapper;

@Service
public class RefundService {

    private final RefundMapper refundMapper;
    private final OrderMapper orderMapper;
    private final NotificationService notificationService;

    RefundService(RefundMapper refundMapper, OrderMapper orderMapper, NotificationService notificationService) {
        this.refundMapper = refundMapper;
        this.orderMapper = orderMapper;
        this.notificationService = notificationService;
    }

    @Transactional
    public RefundMapper.RefundView requestRefund(long userId, String orderNo, BigDecimal amount, String reason) {
        OrderMapper.OrderView order = orderMapper.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "订单不存在"));
        if (order.userId() != userId) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ORDER_NOT_OWNED", "无权操作");
        }
        // 验证可退金额
        OrderMapper.AmountView amt = orderMapper.findAmount(order.id())
                .orElseThrow(() -> new BusinessException("ORDER_AMOUNT_NOT_FOUND", "订单金额不存在"));
        BigDecimal refundable = amt.payableAmount().subtract(amt.refundedAmount());
        if (amount.compareTo(refundable) > 0) {
            throw new BusinessException("REFUND_AMOUNT_EXCEED", "退款金额超出可退范围");
        }

        long id = IdWorker.getId();
        String refundNo = "RF" + System.currentTimeMillis() + String.format("%04d", (int)(Math.random() * 10000));
        refundMapper.insert(id, refundNo, order.id(), 0, amount, reason);
        return refundMapper.findByRefundNo(refundNo).orElseThrow();
    }

    @Transactional
    public void approveRefund(long operatorId, String refundNo) {
        RefundMapper.RefundView refund = refundMapper.findByRefundNo(refundNo)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "REFUND_NOT_FOUND", "退款单不存在"));
        if (!"PENDING".equals(refund.status())) {
            throw new BusinessException("REFUND_NOT_PENDING", "该退款单不在待审核状态");
        }
        refundMapper.approve(refundNo, "PROCESSING", operatorId);
        // 模拟退款成功
        String mockWechatRefundId = "MOCK_REFUND_" + System.currentTimeMillis();
        refundMapper.markSuccess(refundNo, mockWechatRefundId);
        refundMapper.addRefundedAmount(refund.orderId(), refund.amount());
        // 通过 orderId 获取订单信息，发送退款成功通知给正确的用户
        OrderMapper.OrderView order = orderMapper.findByOrderId(refund.orderId());
        if (order != null) {
            notificationService.send(
                    order.userId(),
                    "REFUND_SUCCESS", "退款成功", "退款金额 ¥" + refund.amount() + " 已到账", order.orderNo());
        }
    }

    @Transactional
    public void rejectRefund(long operatorId, String refundNo, String reason) {
        RefundMapper.RefundView refund = refundMapper.findByRefundNo(refundNo)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "REFUND_NOT_FOUND", "退款单不存在"));
        if (!"PENDING".equals(refund.status())) {
            throw new BusinessException("REFUND_NOT_PENDING", "该退款单不在待审核状态");
        }
        refundMapper.approve(refundNo, "REJECTED", operatorId);
    }

    public List<RefundMapper.RefundView> listByOrder(long orderId) {
        return refundMapper.findByOrderId(orderId);
    }

    public List<RefundMapper.RefundView> listAll(int page, int size) {
        return refundMapper.findAll(size, page * size);
    }
}
