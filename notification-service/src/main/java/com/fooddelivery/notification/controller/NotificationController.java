package com.fooddelivery.notification.controller;

import com.fooddelivery.notification.dto.NotificationResponse;
import com.fooddelivery.notification.security.NotificationUserPrincipal;
import com.fooddelivery.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/my")
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(
            @AuthenticationPrincipal NotificationUserPrincipal principal) {
        List<NotificationResponse> responses = notificationService.getNotificationsByCustomerId(principal.getCustomerId());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{notificationId}")
    public ResponseEntity<NotificationResponse> getNotificationById(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal NotificationUserPrincipal principal) {
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        NotificationResponse response = notificationService.getNotificationById(notificationId, principal.getCustomerId(), isAdmin);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal NotificationUserPrincipal principal) {
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        NotificationResponse response = notificationService.markAsRead(notificationId, principal.getCustomerId(), isAdmin);
        return ResponseEntity.ok(response);
    }
}
