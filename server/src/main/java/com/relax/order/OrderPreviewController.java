package com.relax.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.relax.auth.CurrentUser;
import com.relax.common.api.ApiResponse;

@RestController
@RequestMapping("/api/v1")
public class OrderPreviewController {

    private final OrderPreviewService previewService;

    OrderPreviewController(OrderPreviewService previewService) {
        this.previewService = previewService;
    }

    @PostMapping("/orders/preview")
    ApiResponse<OrderPreviewService.OrderPreview> preview(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody PreviewRequest request) {
        return ApiResponse.success(previewService.preview(currentUser.id(),
                new OrderPreviewService.PreviewRequest(
                        request.projectId(), request.technicianId(), request.addressId(),
                        request.serviceDate(), request.startTime(), request.couponId())));
    }

    public record PreviewRequest(
            @NotNull Long projectId,
            @NotNull Long technicianId,
            Long addressId,
            @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}") String serviceDate,
            @NotBlank @Pattern(regexp = "\\d{2}:\\d{2}") String startTime,
            Long couponId) {}
}
