package com.relax.refund;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/v1/admin/refunds")
@PreAuthorize("hasAuthority('order:refund')")
public class AdminRefundController {

    private final RefundService refundService;

    AdminRefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @GetMapping
    ApiResponse<List<RefundMapper.RefundView>> list(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        return ApiResponse.success(refundService.listAll(page, size));
    }

    @PostMapping("/{refundNo}/approve")
    ApiResponse<Void> approve(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable String refundNo) {
        refundService.approveRefund(currentUser.id(), refundNo);
        return ApiResponse.success(null);
    }

    @PostMapping("/{refundNo}/reject")
    ApiResponse<Void> reject(@AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable String refundNo, @Valid @RequestBody RejectRequest request) {
        refundService.rejectRefund(currentUser.id(), refundNo, request.reason());
        return ApiResponse.success(null);
    }

    public record RejectRequest(@Size(max = 500) String reason) {}
}
