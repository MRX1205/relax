package com.relax.content;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.relax.auth.CurrentUser;
import com.relax.common.api.ApiResponse;

@RestController
@RequestMapping("/api/v1")
public class AgreementController {

    private final AgreementService agreementService;

    AgreementController(AgreementService agreementService) {
        this.agreementService = agreementService;
    }

    @GetMapping("/agreements/{type}")
    ApiResponse<AgreementMapper.Agreement> current(@PathVariable String type) {
        return ApiResponse.success(agreementService.current(type));
    }

    @GetMapping("/me/agreements")
    ApiResponse<List<AgreementMapper.Consent>> consents(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.success(agreementService.consents(currentUser.id()));
    }

    @PostMapping("/me/agreements/{agreementId}/consent")
    ApiResponse<AgreementMapper.Consent> consent(@AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable long agreementId, @Valid @RequestBody AgreementService.ConsentRequest request) {
        return ApiResponse.success(agreementService.consent(currentUser.id(), agreementId, request.context()));
    }
}
