package com.fooddelivery.notification.service;

import com.fooddelivery.common.event.OrderCreatedEvent;
import com.fooddelivery.notification.dto.NotificationResponse;
import com.fooddelivery.notification.entity.Notification;
import com.fooddelivery.notification.entity.NotificationStatus;
import com.fooddelivery.notification.entity.NotificationType;
import com.fooddelivery.notification.exception.InvalidNotificationException;
import com.fooddelivery.notification.exception.NotificationNotFoundException;
import com.fooddelivery.notification.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    @DisplayName("Process OrderCreatedEvent creates and persists notification with correct fields and formatted message")
    void testProcessOrderCreatedEventSuccess() {
        OrderCreatedEvent event = new OrderCreatedEvent(
                100L, 5L, new BigDecimal("49.99"), "CONFIRMED", LocalDateTime.now()
        );

        when(notificationRepository.existsByOrderIdAndType(100L, NotificationType.ORDER_CREATED)).thenReturn(false);

        Notification savedEntity = new Notification(
                100L, 5L, NotificationType.ORDER_CREATED,
                "Order #100 has been successfully created. Total amount: 49.99",
                NotificationStatus.UNREAD
        );
        savedEntity.setNotificationId(1L);
        savedEntity.setCreatedAt(LocalDateTime.now());

        when(notificationRepository.save(any(Notification.class))).thenReturn(savedEntity);

        NotificationResponse response = notificationService.processOrderCreatedEvent(event);

        assertThat(response).isNotNull();
        assertThat(response.getNotificationId()).isEqualTo(1L);
        assertThat(response.getOrderId()).isEqualTo(100L);
        assertThat(response.getCustomerId()).isEqualTo(5L);
        assertThat(response.getType()).isEqualTo(NotificationType.ORDER_CREATED);
        assertThat(response.getStatus()).isEqualTo(NotificationStatus.UNREAD);
        assertThat(response.getMessage()).isEqualTo("Order #100 has been successfully created. Total amount: 49.99");

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    @DisplayName("Duplicate OrderCreatedEvent is idempotent - returns existing notification without persisting duplicate")
    void testProcessOrderCreatedEventDuplicateIdempotency() {
        OrderCreatedEvent event = new OrderCreatedEvent(
                100L, 5L, new BigDecimal("49.99"), "CONFIRMED", LocalDateTime.now()
        );

        when(notificationRepository.existsByOrderIdAndType(100L, NotificationType.ORDER_CREATED)).thenReturn(true);

        Notification existing = new Notification(
                100L, 5L, NotificationType.ORDER_CREATED,
                "Order #100 has been successfully created. Total amount: 49.99",
                NotificationStatus.UNREAD
        );
        existing.setNotificationId(1L);
        existing.setCreatedAt(LocalDateTime.now());

        when(notificationRepository.findByOrderIdAndType(100L, NotificationType.ORDER_CREATED))
                .thenReturn(Optional.of(existing));

        NotificationResponse response = notificationService.processOrderCreatedEvent(event);

        assertThat(response).isNotNull();
        assertThat(response.getNotificationId()).isEqualTo(1L);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Concurrent duplicate event handling via DataIntegrityViolationException returns existing notification")
    void testProcessOrderCreatedEventConcurrentDuplicateHandled() {
        OrderCreatedEvent event = new OrderCreatedEvent(
                200L, 6L, new BigDecimal("19.99"), "CONFIRMED", LocalDateTime.now()
        );

        when(notificationRepository.existsByOrderIdAndType(200L, NotificationType.ORDER_CREATED)).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenThrow(new DataIntegrityViolationException("Duplicate"));

        Notification existing = new Notification(
                200L, 6L, NotificationType.ORDER_CREATED,
                "Order #200 has been successfully created. Total amount: 19.99",
                NotificationStatus.UNREAD
        );
        existing.setNotificationId(2L);
        when(notificationRepository.findByOrderIdAndType(200L, NotificationType.ORDER_CREATED))
                .thenReturn(Optional.of(existing));

        NotificationResponse response = notificationService.processOrderCreatedEvent(event);

        assertThat(response).isNotNull();
        assertThat(response.getNotificationId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Invalid event (null event or missing orderId/customerId) throws InvalidNotificationException")
    void testInvalidEventThrowsException() {
        assertThatThrownBy(() -> notificationService.processOrderCreatedEvent(null))
                .isInstanceOf(InvalidNotificationException.class);

        OrderCreatedEvent missingOrderId = new OrderCreatedEvent(null, 5L, new BigDecimal("10.00"), "CONFIRMED", LocalDateTime.now());
        assertThatThrownBy(() -> notificationService.processOrderCreatedEvent(missingOrderId))
                .isInstanceOf(InvalidNotificationException.class);

        OrderCreatedEvent missingCustomerId = new OrderCreatedEvent(1L, null, new BigDecimal("10.00"), "CONFIRMED", LocalDateTime.now());
        assertThatThrownBy(() -> notificationService.processOrderCreatedEvent(missingCustomerId))
                .isInstanceOf(InvalidNotificationException.class);
    }

    @Test
    @DisplayName("getNotificationsByCustomerId returns list belonging to customer")
    void testGetNotificationsByCustomerId() {
        Notification notif = new Notification(10L, 1L, NotificationType.ORDER_CREATED, "Msg", NotificationStatus.UNREAD);
        notif.setNotificationId(1L);
        when(notificationRepository.findByCustomerIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(notif));

        List<NotificationResponse> list = notificationService.getNotificationsByCustomerId(1L);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getOrderId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Owner customer can view their notification")
    void testGetNotificationByIdOwnerSuccess() {
        Notification notif = new Notification(10L, 1L, NotificationType.ORDER_CREATED, "Msg", NotificationStatus.UNREAD);
        notif.setNotificationId(1L);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notif));

        NotificationResponse response = notificationService.getNotificationById(1L, 1L, false);
        assertThat(response).isNotNull();
        assertThat(response.getNotificationId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Non-owner customer cannot view another customer's notification - throws AccessDeniedException")
    void testGetNotificationByIdNonOwnerForbidden() {
        Notification notif = new Notification(10L, 2L, NotificationType.ORDER_CREATED, "Msg", NotificationStatus.UNREAD);
        notif.setNotificationId(1L);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notif));

        assertThatThrownBy(() -> notificationService.getNotificationById(1L, 1L, false))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You are not authorized to view this notification");
    }

    @Test
    @DisplayName("Admin can view any notification")
    void testGetNotificationByIdAdminSuccess() {
        Notification notif = new Notification(10L, 2L, NotificationType.ORDER_CREATED, "Msg", NotificationStatus.UNREAD);
        notif.setNotificationId(1L);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notif));

        NotificationResponse response = notificationService.getNotificationById(1L, 999L, true);
        assertThat(response).isNotNull();
        assertThat(response.getNotificationId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Notification not found throws NotificationNotFoundException")
    void testGetNotificationByIdNotFound() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.getNotificationById(999L, 1L, false))
                .isInstanceOf(NotificationNotFoundException.class)
                .hasMessageContaining("Notification not found with id: 999");
    }

    @Test
    @DisplayName("markAsRead updates status from UNREAD to READ for owner")
    void testMarkAsReadOwnerSuccess() {
        Notification notif = new Notification(10L, 1L, NotificationType.ORDER_CREATED, "Msg", NotificationStatus.UNREAD);
        notif.setNotificationId(1L);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notif));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response = notificationService.markAsRead(1L, 1L, false);
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(NotificationStatus.READ);
    }

    @Test
    @DisplayName("markAsRead for non-owner throws AccessDeniedException")
    void testMarkAsReadNonOwnerForbidden() {
        Notification notif = new Notification(10L, 2L, NotificationType.ORDER_CREATED, "Msg", NotificationStatus.UNREAD);
        notif.setNotificationId(1L);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notif));

        assertThatThrownBy(() -> notificationService.markAsRead(1L, 1L, false))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You are not authorized to modify this notification");
    }
}
