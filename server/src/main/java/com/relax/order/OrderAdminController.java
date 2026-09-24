package com.relax.order;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.relax.auth.CurrentUser;
import com.relax.common.api.ApiResponse;

@RestController
@RequestMapping("/api/v1/admin/orders")
@PreAuthorize("hasAuthority('order:manage') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class OrderAdminController {

    private final OrderService orderService;

    OrderAdminController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    ApiResponse<List<OrderMapper.OrderListItem>> listOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long technicianId,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String month,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        return ApiResponse.success(orderService.listAdminOrders(status, technicianId, date, month, page, size));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long technicianId,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String month) {
        List<OrderMapper.OrderListItem> orders = orderService.listAdminOrders(status, technicianId, date, month, 0, 5000);
        byte[] xlsx = orderService.generateOrdersExcel(orders);
        String label = (date != null && !date.isBlank()) ? date : ((month != null && !month.isBlank()) ? month : "all");
        String filename = "orders-" + label + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(xlsx);
    }

    @GetMapping("/{orderNo}")
    ApiResponse<OrderService.OrderDetailView> getOrder(@PathVariable String orderNo) {
        return ApiResponse.success(orderService.getOrderDetail(orderNo));
    }

    @PostMapping("/{orderNo}/cancel")
    ApiResponse<Void> cancelOrder(@AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable String orderNo, @Valid @RequestBody AdminCancelRequest request) {
        orderService.adminCancelOrder(currentUser.id(), orderNo, request.reason());
        return ApiResponse.success(null);
    }

    @PutMapping("/{orderNo}/note")
    ApiResponse<Void> updateNote(@PathVariable String orderNo, @Valid @RequestBody NoteRequest request) {
        orderService.updateNote(orderNo, request.note());
        return ApiResponse.success(null);
    }

    public record AdminCancelRequest(@Size(max = 500) String reason) {}

    public record NoteRequest(@Size(max = 500) String note) {}
}
