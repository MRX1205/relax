package com.relax.order;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.relax.auth.CurrentUser;
import com.relax.common.api.ApiResponse;

@RestController
@RequestMapping("/api/v1/technician/orders")
public class TechnicianOrderController {

    private final OrderService orderService;
    private final TechnicianFulfillmentService fulfillmentService;
    private final OrderMapper orderMapper;

    TechnicianOrderController(OrderService orderService, TechnicianFulfillmentService fulfillmentService, OrderMapper orderMapper) {
        this.orderService = orderService;
        this.fulfillmentService = fulfillmentService;
        this.orderMapper = orderMapper;
    }

    @GetMapping("/stats")
    ApiResponse<Map<String, Object>> getStats(@AuthenticationPrincipal CurrentUser currentUser) {
        long techId = fulfillmentService.getTechnicianId(currentUser.id());
        int pending = orderMapper.countPendingOrders(techId);
        int today = orderMapper.countTodayOrders(techId, LocalDate.now());
        return ApiResponse.success(Map.of("pending", pending, "today", today));
    }

    @GetMapping
    ApiResponse<List<OrderMapper.OrderListItem>> listOrders(@AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        long techId = fulfillmentService.getTechnicianId(currentUser.id());
        return ApiResponse.success(orderService.listTechnicianOrders(techId, page, size));
    }

    @GetMapping("/{orderNo}")
    ApiResponse<OrderService.OrderDetailView> getOrder(@AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable String orderNo) {
        return ApiResponse.success(orderService.getOrderDetail(orderNo));
    }

    @PostMapping("/{orderNo}/accept")
    ApiResponse<Void> accept(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable String orderNo) {
        fulfillmentService.acceptOrder(currentUser.id(), orderNo);
        return ApiResponse.success(null);
    }

    @PostMapping("/{orderNo}/reject")
    ApiResponse<Void> reject(@AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable String orderNo, @Valid @RequestBody RejectRequest request) {
        fulfillmentService.rejectOrder(currentUser.id(), orderNo, request.reason());
        return ApiResponse.success(null);
    }

    @PostMapping("/{orderNo}/depart")
    ApiResponse<Void> depart(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable String orderNo) {
        fulfillmentService.departOrder(currentUser.id(), orderNo);
        return ApiResponse.success(null);
    }

    @PostMapping("/{orderNo}/arrive")
    ApiResponse<Void> arrive(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable String orderNo) {
        fulfillmentService.arriveOrder(currentUser.id(), orderNo);
        return ApiResponse.success(null);
    }

    @PostMapping("/{orderNo}/start")
    ApiResponse<Void> startService(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable String orderNo) {
        fulfillmentService.startService(currentUser.id(), orderNo);
        return ApiResponse.success(null);
    }

    @PostMapping("/{orderNo}/complete")
    ApiResponse<Void> complete(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable String orderNo) {
        fulfillmentService.completeService(currentUser.id(), orderNo);
        return ApiResponse.success(null);
    }

    public record RejectRequest(@Size(max = 500) String reason) {}
}
