package com.relax.refund;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.relax.auth.CurrentUser;
import com.relax.common.api.ApiResponse;
import com.relax.common.api.BusinessException;
import com.relax.order.OrderMapper;

@RestController
@RequestMapping("/api/v1")
public class RefundController {

    private final RefundService refundService;
    private final OrderMapper orderMapper;

    RefundController(RefundService refundService, OrderMapper orderMapper) {
        this.refundService = refundService;
        this.orderMapper = orderMapper;
    }

    @PostMapping("/orders/{orderNo}/refunds")
    ApiResponse<RefundMapper.RefundView> requestRefund(@AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable String orderNo, @Valid @RequestBody RefundRequest request) {
        return ApiResponse.success(refundService.requestRefund(currentUser.id(), orderNo,
                request.amount(), request.reason()));
    }

    @GetMapping("/orders/{orderNo}/refunds")
    ApiResponse<List<RefundMapper.RefundView>> listRefunds(@PathVariable String orderNo) {
        long orderId = orderMapper.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "订单不存在"))
                .id();
        return ApiResponse.success(refundService.listByOrder(orderId));
    }

    public record RefundRequest(@NotNull @DecimalMin("0.01") BigDecimal amount, @NotBlank String reason) {}
}
