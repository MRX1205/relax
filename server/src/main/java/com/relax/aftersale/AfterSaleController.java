package com.relax.aftersale;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.relax.auth.CurrentUser;
import com.relax.common.api.ApiResponse;

@RestController
@RequestMapping("/api/v1")
public class AfterSaleController {

    private final AfterSaleService afterSaleService;

    AfterSaleController(AfterSaleService afterSaleService) {
        this.afterSaleService = afterSaleService;
    }

    @PostMapping("/orders/{orderNo}/after-sales")
    ApiResponse<AfterSaleMapper.CaseView> createCase(@AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable String orderNo, @Valid @RequestBody CreateCaseRequest request) {
        return ApiResponse.success(afterSaleService.createCase(currentUser.id(), orderNo,
                request.type(), request.content()));
    }

    public record CreateCaseRequest(
            @NotBlank @Size(max = 40) String type,
            @NotBlank @Size(max = 1000) String content) {}
}
