package com.relax.order;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.relax.common.api.BusinessException;
import com.relax.notification.NotificationService;

@Service
public class ReassignmentService {

    private final OrderMapper orderMapper;
    private final NotificationService notificationService;

    ReassignmentService(OrderMapper orderMapper, NotificationService notificationService) {
        this.orderMapper = orderMapper;
        this.notificationService = notificationService;
    }

    @Transactional
    public void reassign(long operatorId, String orderNo, long newTechnicianId, String reason) {
        OrderMapper.OrderView order = orderMapper.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "订单不存在"));

        String status = order.status();
        if (!"PAID".equals(status) && !"REJECTED".equals(status)) {
            throw new BusinessException("ORDER_NOT_REASSIGNABLE", "当前状态不可改派");
        }

        long fromTechId = order.technicianId();
        if (fromTechId == newTechnicianId) {
            throw new BusinessException("SAME_TECHNICIAN", "不能改派给同一技师");
        }

        // 记录改派
        long reassignId = IdWorker.getId();
        orderMapper.insertReassignment(reassignId, order.id(), fromTechId, newTechnicianId, reason, operatorId, true);

        // 更新订单技师
        String expectedStatus = status;
        if (orderMapper.transitionStatus(orderNo, expectedStatus, "PAID", order.version()) == 0) {
            // 如果状态不是PAID，先尝试直接更新技师
            if (orderMapper.reassignTechnician(orderNo, newTechnicianId, order.version()) == 0) {
                throw new BusinessException("ORDER_STATE_CHANGED", "订单状态已变更");
            }
        } else {
            orderMapper.reassignTechnician(orderNo, newTechnicianId, order.version() + 1);
        }

        logStatus(order.id(), status, "REASSIGNED", "ADMIN", operatorId,
                "改派：" + fromTechId + " → " + newTechnicianId + "，原因：" + reason);

        notificationService.send(order.userId(), "ORDER_REASSIGNED", "订单已改派",
                "您的订单已改派给其他技师", orderNo);
    }

    private void logStatus(long orderId, String from, String to, String operatorType, Long operatorId, String reason) {
        long logId = IdWorker.getId();
        orderMapper.insertStatusLog(logId, orderId, from, to, operatorType, operatorId, reason);
    }
}
