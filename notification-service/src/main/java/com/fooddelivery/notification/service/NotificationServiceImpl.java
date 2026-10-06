package com.fooddelivery.notification.service;

import com.fooddelivery.common.event.OrderCreatedEvent;
import com.fooddelivery.notification.dto.NotificationResponse;
import com.fooddelivery.notification.entity.Notification;
import com.fooddelivery.notification.entity.NotificationStatus;
import com.fooddelivery.notification.entity.NotificationType;
import com.fooddelivery.notification.exception.InvalidNotificationException;
import com.fooddelivery.notification.exception.NotificationNotFoundException;
import com.fooddelivery.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Transactional
    public NotificationResponse processOrderCreatedEvent(OrderCreatedEvent event) {
        if (event == null || event.getOrderId() == null || event.getCustomerId() == null) {
            log.warn("Received invalid OrderCreatedEvent: missing orderId or customerId");
            throw new InvalidNotificationException("Event must contain orderId and customerId");
        }

        Long orderId = event.getOrderId();
        Long customerId = event.getCustomerId();

        // Idempotency check: verify if notification already exists
        if (notificationRepository.existsByOrderIdAndType(orderId, NotificationType.ORDER_CREATED)) {
            log.info("Notification for orderId: {} and type: ORDER_CREATED already exists. Skipping duplicate event.", orderId);
            return notificationRepository.findByOrderIdAndType(orderId, NotificationType.ORDER_CREATED)
                    .map(this::mapToResponse)
                    .orElse(null);
        }

        String totalAmountStr = event.getTotalAmount() != null ? event.getTotalAmount().toPlainString() : "0.00";
        String message = "Order #" + orderId + " has been successfully created. Total amount: " + totalAmountStr;

        Notification notification = new Notification(
                orderId,
                customerId,
                NotificationType.ORDER_CREATED,
                message,
                NotificationStatus.UNREAD
        );

        try {
            Notification saved = notificationRepository.save(notification);
            log.info("Persisted notificationId: {} for orderId: {} and customerId: {}", saved.getNotificationId(), orderId, customerId);
            return mapToResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Concurrent duplicate notification detected for orderId: {}. Fetching existing record.", orderId);
            return notificationRepository.findByOrderIdAndType(orderId, NotificationType.ORDER_CREATED)
                    .map(this::mapToResponse)
                    .orElseThrow(() -> ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByCustomerId(Long customerId) {
        log.info("Fetching notifications for customerId: {}", customerId);
        return notificationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationResponse getNotificationById(Long notificationId, Long currentUserId, boolean isAdmin) {
        log.info("Fetching notificationId: {} for userId: {} (isAdmin: {})", notificationId, currentUserId, isAdmin);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found with id: " + notificationId));

        if (!isAdmin && !notification.getCustomerId().equals(currentUserId)) {
            log.warn("Access denied: userId: {} attempted to access notificationId: {} owned by customerId: {}",
                    currentUserId, notificationId, notification.getCustomerId());
            throw new AccessDeniedException("You are not authorized to view this notification");
        }

        return mapToResponse(notification);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long notificationId, Long currentUserId, boolean isAdmin) {
        log.info("Marking notificationId: {} as READ by userId: {} (isAdmin: {})", notificationId, currentUserId, isAdmin);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found with id: " + notificationId));

        if (!isAdmin && !notification.getCustomerId().equals(currentUserId)) {
            log.warn("Access denied: userId: {} attempted to modify notificationId: {} owned by customerId: {}",
                    currentUserId, notificationId, notification.getCustomerId());
            throw new AccessDeniedException("You are not authorized to modify this notification");
        }

        if (notification.getStatus() != NotificationStatus.READ) {
            notification.setStatus(NotificationStatus.READ);
            notification = notificationRepository.save(notification);
            log.info("NotificationId: {} status updated to READ", notificationId);
        }

        return mapToResponse(notification);
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return new NotificationResponse(
                notification.getNotificationId(),
                notification.getOrderId(),
                notification.getCustomerId(),
                notification.getType(),
                notification.getMessage(),
                notification.getStatus(),
                notification.getCreatedAt()
        );
    }
}
