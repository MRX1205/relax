package com.relax.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.relax.auth.CurrentUser;
import com.relax.common.api.ApiResponse;

@RestController
@RequestMapping("/api/v1/admin/orders/{orderNo}")
@PreAuthorize("hasAuthority('order:manage')")
public class ReassignmentController {

    private final ReassignmentService reassignmentService;

    ReassignmentController(ReassignmentService reassignmentService) {
        this.reassignmentService = reassignmentService;
    }

    @PostMapping("/reassign")
    ApiResponse<Void> reassign(@AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable String orderNo, @Valid @RequestBody ReassignRequest request) {
        reassignmentService.reassign(currentUser.id(), orderNo, request.newTechnicianId(), request.reason());
        return ApiResponse.success(null);
    }

    public record ReassignRequest(@NotNull Long newTechnicianId, @Size(max = 500) String reason) {}
}
