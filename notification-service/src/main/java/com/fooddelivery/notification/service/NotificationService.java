package com.fooddelivery.notification.service;

import com.fooddelivery.common.event.OrderCreatedEvent;
import com.fooddelivery.notification.dto.NotificationResponse;

import java.util.List;

public interface NotificationService {

    NotificationResponse processOrderCreatedEvent(OrderCreatedEvent event);

    List<NotificationResponse> getNotificationsByCustomerId(Long customerId);

    NotificationResponse getNotificationById(Long notificationId, Long currentUserId, boolean isAdmin);

    NotificationResponse markAsRead(Long notificationId, Long currentUserId, boolean isAdmin);
}
