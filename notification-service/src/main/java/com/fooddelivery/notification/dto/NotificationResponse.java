package com.fooddelivery.notification.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fooddelivery.notification.entity.NotificationStatus;
import com.fooddelivery.notification.entity.NotificationType;

import java.time.LocalDateTime;

public class NotificationResponse {

    private Long notificationId;
    private Long orderId;
    private Long customerId;
    private NotificationType type;
    private String message;
    private NotificationStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    public NotificationResponse() {
    }

    public NotificationResponse(Long notificationId, Long orderId, Long customerId,
                                NotificationType type, String message,
                                NotificationStatus status, LocalDateTime createdAt) {
        this.notificationId = notificationId;
        this.orderId = orderId;
        this.customerId = customerId;
        this.type = type;
        this.message = message;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Long notificationId) {
        this.notificationId = notificationId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
