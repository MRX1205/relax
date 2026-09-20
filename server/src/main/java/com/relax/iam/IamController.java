package com.relax.iam;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.relax.auth.CurrentUser;
import com.relax.common.api.ApiResponse;

@RestController
@RequestMapping("/api/v1/admin/access")
@PreAuthorize("hasAuthority('admin:manage')")
public class IamController {

    private final IamService iamService;

    IamController(IamService iamService) {
        this.iamService = iamService;
    }

    @GetMapping("/users")
    ApiResponse<List<IamService.AccessUserView>> users(@RequestParam(required = false) String keyword) {
        return ApiResponse.success(iamService.findUsers(keyword));
    }

    @GetMapping("/groups")
    ApiResponse<List<IamService.GroupView>> groups() {
        return ApiResponse.success(iamService.findGroups());
    }

    @PutMapping("/users/{userId}")
    ApiResponse<IamService.AccessUserView> updateAccess(@AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable long userId, @Valid @RequestBody IamService.AdminAccessRequest request,
            HttpServletRequest servletRequest) {
        return ApiResponse.success(iamService.updateAdminAccess(currentUser.id(), userId, request.enabled(),
                request.groupCodes(), servletRequest.getRemoteAddr()));
    }

    @PutMapping("/users/{userId}/status")
    ApiResponse<IamService.AccessUserView> updateStatus(@AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable long userId, @Valid @RequestBody IamService.AccountStatusRequest request,
            HttpServletRequest servletRequest) {
        return ApiResponse.success(iamService.updateStatus(currentUser.id(), userId, request.status(),
                servletRequest.getRemoteAddr()));
    }
}
