package com.relax.order;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.relax.catalog.TechnicianPublicMapper;
import com.relax.common.api.BusinessException;
import com.relax.coupon.CouponService;
import com.relax.region.RegionMapper;
import com.relax.schedule.ScheduleMapper;

@Service
public class OrderService {

    private static final BigDecimal TRAVEL_FEE = new BigDecimal("20.00");
    private static final int PAYMENT_TIMEOUT_MINUTES = 15;

    private final OrderMapper orderMapper;
    private final TechnicianPublicMapper techMapper;
    private final ScheduleMapper scheduleMapper;
    private final RegionMapper regionMapper;
    private final CouponService couponService;
    private final com.relax.technician.TechnicianAuditMapper auditMapper;

    OrderService(OrderMapper orderMapper, TechnicianPublicMapper techMapper,
            ScheduleMapper scheduleMapper, RegionMapper regionMapper, CouponService couponService,
            com.relax.technician.TechnicianAuditMapper auditMapper) {
        this.orderMapper = orderMapper;
        this.techMapper = techMapper;
        this.scheduleMapper = scheduleMapper;
        this.regionMapper = regionMapper;
        this.couponService = couponService;
        this.auditMapper = auditMapper;
    }

    @Transactional
    public OrderMapper.OrderView createOrder(long userId, CreateOrderRequest request) {
        TechnicianPublicMapper.TechnicianProjectItem projectItem = techMapper
                .findProjectsForTechnician(request.technicianId()).stream()
                .filter(p -> p.projectId() == request.projectId())
                .findFirst()
                .orElseThrow(() -> new BusinessException("PROJECT_NOT_AVAILABLE", "该技师不提供此项目"));

        RegionMapper.UserAddress address = regionMapper.findAddress(userId, request.addressId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "ADDRESS_NOT_FOUND", "地址不存在"));

        LocalDate serviceDate = LocalDate.parse(request.serviceDate());
        String startTime = request.startTime();
        String endTime = calculateEndTime(startTime, projectItem.durationMinutes());

        List<ScheduleMapper.ScheduleView> schedules = scheduleMapper
                .findByTechnicianAndDate(request.technicianId(), serviceDate);
        boolean slotAvailable = schedules.stream().anyMatch(s ->
                s.startTime().compareTo(startTime) <= 0 && s.endTime().compareTo(endTime) >= 0);
        if (!slotAvailable) {
            throw new BusinessException("SLOT_NOT_AVAILABLE", "所选时段不可用");
        }

        LocalDateTime now = LocalDateTime.now();
        if (orderMapper.countActiveLock(request.technicianId(), serviceDate, startTime, endTime, now) > 0) {
            throw new BusinessException("SLOT_LOCKED", "该时段已被占用，请选择其他时间");
        }

        long orderId = IdWorker.getId();
        String orderNo = generateOrderNo();
        orderMapper.insertOrder(orderId, orderNo, userId, request.technicianId(),
                request.projectId(), serviceDate, startTime, endTime, request.note());

        BigDecimal actualPrice = projectItem.price();
        orderMapper.insertProjectSnapshot(orderId, projectItem.projectName(),
                projectItem.durationMinutes(), BigDecimal.ZERO, actualPrice, actualPrice);
        orderMapper.insertAddressSnapshot(orderId, address.contactName(), address.contactPhone(),
                address.regionName(), address.detail(), address.longitude(), address.latitude());

        BigDecimal subtotal = actualPrice.add(TRAVEL_FEE);
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (request.couponId() != null) {
            discountAmount = couponService.lockCoupon(userId, request.couponId(), orderId, subtotal);
        }
        BigDecimal payableAmount = subtotal.subtract(discountAmount);
        orderMapper.insertAmount(orderId, actualPrice, TRAVEL_FEE, discountAmount, payableAmount);

        long lockId = IdWorker.getId();
        LocalDateTime lockExpire = now.plusMinutes(PAYMENT_TIMEOUT_MINUTES);
        orderMapper.insertLock(lockId, request.technicianId(), serviceDate, startTime, endTime, orderNo, lockExpire);

        logStatus(orderId, null, "PENDING_PAYMENT", "SYSTEM", null, "订单创建");

        return orderMapper.findByOrderNo(orderNo).orElseThrow();
    }

    @Transactional
    public void cancelOrder(long userId, String orderNo, String reason) {
        OrderMapper.OrderView order = requireOrder(orderNo);
        if (order.userId() != userId) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ORDER_NOT_OWNED", "无权操作该订单");
        }
        if (!"PENDING_PAYMENT".equals(order.status())) {
            throw new BusinessException("ORDER_NOT_CANCELLABLE", "当前状态不可取消");
        }
        if (orderMapper.transitionStatus(orderNo, "PENDING_PAYMENT", "CANCELLED", order.version()) == 0) {
            throw new BusinessException("ORDER_STATE_CHANGED", "订单状态已变更，请刷新");
        }
        orderMapper.releaseLock(orderNo);
        logStatus(order.id(), "PENDING_PAYMENT", "CANCELLED", "USER", userId, reason);
    }

    @Transactional
    public void adminCancelOrder(long operatorId, String orderNo, String reason) {
        OrderMapper.OrderView order = requireOrder(orderNo);
        String fromStatus = order.status();
        if (orderMapper.transitionWithReason(orderNo, fromStatus, "CANCELLED", reason, order.version()) == 0) {
            throw new BusinessException("ORDER_STATE_CHANGED", "订单状态已变更");
        }
        orderMapper.releaseLock(orderNo);
        logStatus(order.id(), fromStatus, "CANCELLED", "ADMIN", operatorId, reason);
    }

    @Transactional
    public void updateNote(String orderNo, String note) {
        orderMapper.updateNote(orderNo, note);
    }

    public OrderDetailView getOrderDetail(String orderNo) {
        OrderMapper.OrderView order = requireOrder(orderNo);
        OrderMapper.ProjectSnapshot projectSnap = orderMapper.findProjectSnapshot(order.id()).orElse(null);
        OrderMapper.AddressSnapshot addressSnap = orderMapper.findAddressSnapshot(order.id()).orElse(null);
        OrderMapper.AmountView amount = orderMapper.findAmount(order.id()).orElse(null);
        List<OrderMapper.StatusLogView> logs = orderMapper.findStatusLogs(order.id());

        String techName = "专业技师";
        String techPhone = "";
        String techAvatar = "";
        var techOpt = auditMapper.findFullProfileById(order.technicianId());
        if (techOpt.isPresent()) {
            techName = techOpt.get().serviceName();
            techPhone = techOpt.get().phone();
            techAvatar = techOpt.get().avatarUrl() != null ? techOpt.get().avatarUrl() : "";
        }
        String custName = addressSnap != null ? addressSnap.contactName() : "客户";
        String custPhone = addressSnap != null ? addressSnap.contactPhone() : "";

        return new OrderDetailView(order, projectSnap, addressSnap, amount, logs,
                techName, techPhone, techAvatar, custName, custPhone);
    }

    public List<OrderMapper.OrderListItem> listUserOrders(long userId, int page, int size) {
        return orderMapper.findEnrichedByUser(userId, size, page * size);
    }

    public List<OrderMapper.OrderListItem> listTechnicianOrders(long technicianId, int page, int size) {
        return orderMapper.findEnrichedByTechnician(technicianId, size, page * size);
    }

    public List<OrderMapper.OrderListItem> listAllOrders(int page, int size) {
        return orderMapper.findEnrichedAll(size, page * size);
    }

    @Transactional
    public void expirePendingOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(PAYMENT_TIMEOUT_MINUTES);
        List<String> expired = orderMapper.findExpiredPendingOrders(cutoff);
        for (String orderNo : expired) {
            Optional<OrderMapper.OrderView> opt = orderMapper.findByOrderNo(orderNo);
            if (opt.isPresent() && "PENDING_PAYMENT".equals(opt.get().status())) {
                if (orderMapper.transitionStatus(orderNo, "PENDING_PAYMENT", "EXPIRED", opt.get().version()) > 0) {
                    orderMapper.releaseLock(orderNo);
                    logStatus(opt.get().id(), "PENDING_PAYMENT", "EXPIRED", "SYSTEM", null, "超时未支付");
                }
            }
        }
    }

    public void markPaid(String orderNo, String transactionId, BigDecimal amount) {
        OrderMapper.OrderView order = requireOrder(orderNo);
        if (!"PENDING_PAYMENT".equals(order.status())) {
            return;
        }
        OrderMapper.AmountView expectedAmount = orderMapper.findAmount(order.id()).orElseThrow();
        if (expectedAmount.payableAmount().compareTo(amount) != 0) {
            throw new BusinessException("PAYMENT_AMOUNT_MISMATCH", "支付金额不一致");
        }
        if (orderMapper.transitionStatus(orderNo, "PENDING_PAYMENT", "PAID", order.version()) == 0) {
            return;
        }
        orderMapper.releaseLock(orderNo);
        logStatus(order.id(), "PENDING_PAYMENT", "PAID", "SYSTEM", null, "支付成功: " + transactionId);
    }

    private OrderMapper.OrderView requireOrder(String orderNo) {
        return orderMapper.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "订单不存在"));
    }

    private void logStatus(long orderId, String from, String to, String operatorType, Long operatorId, String reason) {
        long logId = IdWorker.getId();
        orderMapper.insertStatusLog(logId, orderId, from, to, operatorType, operatorId, reason);
    }

    private String calculateEndTime(String startTime, int durationMinutes) {
        LocalTime start = LocalTime.parse(startTime, DateTimeFormatter.ofPattern("HH:mm"));
        return start.plusMinutes(durationMinutes).format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    private String generateOrderNo() {
        return "RX" + System.currentTimeMillis() + String.format("%04d", (int)(Math.random() * 10000));
    }

    public record CreateOrderRequest(long projectId, long technicianId, long addressId,
            String serviceDate, String startTime, String note, Long couponId) {}

    public record OrderDetailView(
            OrderMapper.OrderView order,
            OrderMapper.ProjectSnapshot projectSnapshot,
            OrderMapper.AddressSnapshot addressSnapshot,
            OrderMapper.AmountView amount,
            List<OrderMapper.StatusLogView> statusLogs,
            String technicianName,
            String technicianPhone,
            String technicianAvatarUrl,
            String customerName,
            String customerPhone) {}
}
