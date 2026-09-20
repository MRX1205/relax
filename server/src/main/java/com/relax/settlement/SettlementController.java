package com.relax.settlement;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.relax.auth.CurrentUser;
import com.relax.common.api.ApiResponse;
import com.relax.technician.TechnicianAuditMapper;

@RestController
@RequestMapping("/api/v1/technician")
public class SettlementController {

    private final SettlementService settlementService;
    private final TechnicianAuditMapper techMapper;

    SettlementController(SettlementService settlementService, TechnicianAuditMapper techMapper) {
        this.settlementService = settlementService;
        this.techMapper = techMapper;
    }

    @GetMapping("/incomes")
    ApiResponse<List<SettlementMapper.IncomeView>> listIncome(@AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        long techId = getTechId(currentUser.id());
        return ApiResponse.success(settlementService.listIncome(techId, page, size));
    }

    @GetMapping("/settlements")
    ApiResponse<List<SettlementMapper.SettlementView>> listSettlements(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        long techId = getTechId(currentUser.id());
        return ApiResponse.success(settlementService.listSettlementsByTech(techId, page, size));
    }

    private long getTechId(long userId) {
        return techMapper.findAllTechnicians().stream()
                .filter(t -> t.userId() == userId)
                .findFirst()
                .map(TechnicianAuditMapper.TechnicianBrief::id)
                .orElse(0L);
    }
}
