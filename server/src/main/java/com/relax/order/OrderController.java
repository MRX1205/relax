package com.relax.order;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

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
@RequestMapping("/api/v1")
public class OrderController {

    private final OrderService orderService;

    OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/orders")
    ApiResponse<OrderService.OrderDetailView> createOrder(@AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody CreateOrderRequest request) {
        OrderMapper.OrderView created = orderService.createOrder(currentUser.id(),
                new OrderService.CreateOrderRequest(request.projectId(), request.technicianId(),
                        request.addressId(), request.serviceDate(), request.startTime(), request.note(),
                        request.couponId()));
        return ApiResponse.success(orderService.getOrderDetail(created.orderNo()));
    }

    @GetMapping("/orders")
    ApiResponse<List<OrderMapper.OrderView>> listOrders(@AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        return ApiResponse.success(orderService.listUserOrders(currentUser.id(), page, size));
    }

    @GetMapping("/orders/{orderNo}")
    ApiResponse<OrderService.OrderDetailView> getOrder(@AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable String orderNo) {
        return ApiResponse.success(orderService.getOrderDetail(orderNo));
    }

    @PostMapping("/orders/{orderNo}/cancel")
    ApiResponse<Void> cancelOrder(@AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable String orderNo, @Valid @RequestBody CancelRequest request) {
        orderService.cancelOrder(currentUser.id(), orderNo, request.reason());
        return ApiResponse.success(null);
    }

    public record CreateOrderRequest(
            @NotNull Long projectId,
            @NotNull Long technicianId,
            @NotNull Long addressId,
            @NotBlank String serviceDate,
            @NotBlank String startTime,
            String note,
            Long couponId) {}

    public record CancelRequest(String reason) {}
}
