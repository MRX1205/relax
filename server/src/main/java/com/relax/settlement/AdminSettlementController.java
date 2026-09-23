package com.relax.settlement;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

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
@RequestMapping("/api/v1/admin/settlements")
@PreAuthorize("hasAuthority('settlement:manage') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminSettlementController {

    private final SettlementService settlementService;

    AdminSettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @GetMapping
    ApiResponse<List<SettlementMapper.SettlementView>> list(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        return ApiResponse.success(settlementService.listAllSettlements(page, size));
    }

    @PostMapping("/{id}/pay")
    ApiResponse<Void> markPaid(@AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable long id, @Valid @RequestBody PayRequest request) {
        settlementService.markPaid(currentUser.id(), id, request.referenceNo(), request.proofFileId());
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/void")
    ApiResponse<Void> markVoid(@PathVariable long id) {
        settlementService.markVoid(id);
        return ApiResponse.success(null);
    }

    public record PayRequest(@NotBlank String referenceNo, Long proofFileId) {}
}
