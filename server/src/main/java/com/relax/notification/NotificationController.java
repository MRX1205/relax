package com.relax.notification;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.relax.auth.CurrentUser;
import com.relax.common.api.ApiResponse;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    ApiResponse<List<NotificationMapper.NotificationView>> list(@AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        return ApiResponse.success(notificationService.list(currentUser.id(), page, size));
    }

    @PostMapping("/read-all")
    ApiResponse<Integer> markAllRead(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.success(notificationService.markAllRead(currentUser.id()));
    }

    @PostMapping("/{id}/read")
    ApiResponse<Integer> markRead(@PathVariable long id) {
        return ApiResponse.success(notificationService.markRead(id));
    }

    @GetMapping("/unread-count")
    ApiResponse<Integer> unreadCount(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.success(notificationService.unreadCount(currentUser.id()));
    }

    @PostMapping("/admin/broadcast")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('content:manage') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    ApiResponse<Integer> broadcast(@jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody BroadcastRequest request) {
        int count = notificationService.broadcast(request.type(), request.title(), request.content(), request.targetUserId());
        return ApiResponse.success(count);
    }

    public record BroadcastRequest(
            String type,
            @jakarta.validation.constraints.NotBlank String title,
            @jakarta.validation.constraints.NotBlank String content,
            Long targetUserId) {}
}
