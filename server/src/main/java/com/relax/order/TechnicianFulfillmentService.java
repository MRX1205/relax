package com.relax.order;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.relax.common.api.BusinessException;
import com.relax.notification.NotificationService;
import com.relax.settlement.SettlementService;
import com.relax.technician.TechnicianAuditMapper;

@Service
public class TechnicianFulfillmentService {

    private final OrderMapper orderMapper;
    private final TechnicianAuditMapper techMapper;
    private final NotificationService notificationService;
    private final SettlementService settlementService;

    TechnicianFulfillmentService(OrderMapper orderMapper, TechnicianAuditMapper techMapper,
            NotificationService notificationService, SettlementService settlementService) {
        this.orderMapper = orderMapper;
        this.techMapper = techMapper;
        this.notificationService = notificationService;
        this.settlementService = settlementService;
    }

    public long getTechnicianId(long userId) {
        return techMapper.findAllTechnicians().stream()
                .filter(t -> t.userId() == userId)
                .findFirst()
                .map(TechnicianAuditMapper.TechnicianBrief::id)
                .orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "NOT_TECHNICIAN", "当前账号不是技师"));
    }

    @Transactional
    public void acceptOrder(long userId, String orderNo) {
        long techId = getTechnicianId(userId);
        OrderMapper.OrderView order = requireOrder(orderNo);
        if (order.technicianId() != techId) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ORDER_NOT_ASSIGNED", "该订单未分配给您");
        }
        if (!"PAID".equals(order.status())) {
            throw new BusinessException("ORDER_NOT_ACCEPTABLE", "当前状态不可接单");
        }
        if (orderMapper.acceptOrder(orderNo, "PAID", "ACCEPTED", order.version()) == 0) {
            throw new BusinessException("ORDER_STATE_CHANGED", "订单状态已变更");
        }
        logStatus(order.id(), "PAID", "ACCEPTED", "TECHNICIAN", userId, "技师接单");
        notificationService.send(order.userId(), "ORDER_ACCEPTED", "订单已接单", "技师已接单，正在准备出发", orderNo);
    }

    @Transactional
    public void rejectOrder(long userId, String orderNo, String reason) {
        long techId = getTechnicianId(userId);
        OrderMapper.OrderView order = requireOrder(orderNo);
        if (order.technicianId() != techId) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ORDER_NOT_ASSIGNED", "该订单未分配给您");
        }
        if (!"PAID".equals(order.status())) {
            throw new BusinessException("ORDER_NOT_REJECTABLE", "当前状态不可拒单");
        }
        if (orderMapper.transitionWithReason(orderNo, "PAID", "REJECTED", reason, order.version()) == 0) {
            throw new BusinessException("ORDER_STATE_CHANGED", "订单状态已变更");
        }
        logStatus(order.id(), "PAID", "REJECTED", "TECHNICIAN", userId, reason);
        notificationService.send(order.userId(), "ORDER_REJECTED", "技师拒单", "技师拒绝了您的订单：" + reason, orderNo);
    }

    @Transactional
    public void departOrder(long userId, String orderNo) {
        long techId = getTechnicianId(userId);
        OrderMapper.OrderView order = requireOrder(orderNo);
        if (order.technicianId() != techId) throw new BusinessException(HttpStatus.FORBIDDEN, "ORDER_NOT_ASSIGNED", "无权操作");
        if (!"ACCEPTED".equals(order.status())) throw new BusinessException("ORDER_STATE_INVALID", "当前状态不可出发");
        if (orderMapper.transitionStatus(orderNo, "ACCEPTED", "DEPARTED", order.version()) == 0) throw new BusinessException("ORDER_STATE_CHANGED", "订单状态已变更");
        logStatus(order.id(), "ACCEPTED", "DEPARTED", "TECHNICIAN", userId, "技师出发");
        notificationService.send(order.userId(), "ORDER_DEPARTED", "技师已出发", "技师正在前往您的地址", orderNo);
    }

    @Transactional
    public void arriveOrder(long userId, String orderNo) {
        long techId = getTechnicianId(userId);
        OrderMapper.OrderView order = requireOrder(orderNo);
        if (order.technicianId() != techId) throw new BusinessException(HttpStatus.FORBIDDEN, "ORDER_NOT_ASSIGNED", "无权操作");
        if (!"DEPARTED".equals(order.status())) throw new BusinessException("ORDER_STATE_INVALID", "当前状态不可到达");
        if (orderMapper.transitionStatus(orderNo, "DEPARTED", "ARRIVED", order.version()) == 0) throw new BusinessException("ORDER_STATE_CHANGED", "订单状态已变更");
        logStatus(order.id(), "DEPARTED", "ARRIVED", "TECHNICIAN", userId, "技师到达");
        notificationService.send(order.userId(), "ORDER_ARRIVED", "技师已到达", "技师已到达您的地址，请准备开始服务", orderNo);
    }

    @Transactional
    public void startService(long userId, String orderNo) {
        long techId = getTechnicianId(userId);
        OrderMapper.OrderView order = requireOrder(orderNo);
        if (order.technicianId() != techId) throw new BusinessException(HttpStatus.FORBIDDEN, "ORDER_NOT_ASSIGNED", "无权操作");
        if (!"ARRIVED".equals(order.status())) throw new BusinessException("ORDER_STATE_INVALID", "当前状态不可开始服务");
        if (orderMapper.startService(orderNo, "ARRIVED", "IN_SERVICE", order.version()) == 0) throw new BusinessException("ORDER_STATE_CHANGED", "订单状态已变更");
        logStatus(order.id(), "ARRIVED", "IN_SERVICE", "TECHNICIAN", userId, "开始服务");
    }

    @Transactional
    public void completeService(long userId, String orderNo) {
        long techId = getTechnicianId(userId);
        OrderMapper.OrderView order = requireOrder(orderNo);
        if (order.technicianId() != techId) throw new BusinessException(HttpStatus.FORBIDDEN, "ORDER_NOT_ASSIGNED", "无权操作");
        if (!"IN_SERVICE".equals(order.status())) throw new BusinessException("ORDER_STATE_INVALID", "当前状态不可完成服务");
        if (orderMapper.completeService(orderNo, "IN_SERVICE", "COMPLETED", order.version()) == 0) throw new BusinessException("ORDER_STATE_CHANGED", "订单状态已变更");
        logStatus(order.id(), "IN_SERVICE", "COMPLETED", "TECHNICIAN", userId, "服务完成");
        notificationService.send(order.userId(), "ORDER_COMPLETED", "服务已完成", "请对本次服务进行评价", orderNo);
        // Create income for technician
        OrderMapper.AmountView amount = orderMapper.findAmount(order.id()).orElse(null);
        if (amount != null) {
            settlementService.createIncome(order.id(), techId, amount.projectAmount());
        }
    }

    private OrderMapper.OrderView requireOrder(String orderNo) {
        return orderMapper.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "订单不存在"));
    }

    private void logStatus(long orderId, String from, String to, String operatorType, Long operatorId, String reason) {
        long logId = IdWorker.getId();
        orderMapper.insertStatusLog(logId, orderId, from, to, operatorType, operatorId, reason);
    }
}
