package com.fooddelivery.notification.repository;

import com.fooddelivery.notification.entity.Notification;
import com.fooddelivery.notification.entity.NotificationStatus;
import com.fooddelivery.notification.entity.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@ActiveProfiles("test")
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    @DisplayName("Persist notification and verify retrieval and defaults")
    void testPersistAndRetrieveNotification() {
        Notification notification = new Notification(
                1001L, 10L, NotificationType.ORDER_CREATED,
                "Order #1001 created successfully", NotificationStatus.UNREAD
        );

        Notification saved = notificationRepository.save(notification);

        assertThat(saved.getNotificationId()).isNotNull();
        assertThat(saved.getOrderId()).isEqualTo(1001L);
        assertThat(saved.getCustomerId()).isEqualTo(10L);
        assertThat(saved.getType()).isEqualTo(NotificationType.ORDER_CREATED);
        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.UNREAD);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Unique constraint on (order_id, type) throws DataIntegrityViolationException on duplicate insert")
    void testUniqueConstraintViolation() {
        Notification notif1 = new Notification(
                2002L, 20L, NotificationType.ORDER_CREATED,
                "Order #2002 created", NotificationStatus.UNREAD
        );
        notificationRepository.saveAndFlush(notif1);

        Notification notif2 = new Notification(
                2002L, 20L, NotificationType.ORDER_CREATED,
                "Duplicate Order #2002", NotificationStatus.UNREAD
        );

        assertThrows(DataIntegrityViolationException.class, () -> {
            notificationRepository.saveAndFlush(notif2);
        });
    }

    @Test
    @DisplayName("findByCustomerIdOrderByCreatedAtDesc retrieves notifications in descending order")
    void testFindByCustomerIdOrderByCreatedAtDesc() throws InterruptedException {
        Notification notif1 = new Notification(3001L, 30L, NotificationType.ORDER_CREATED, "First", NotificationStatus.UNREAD);
        notificationRepository.saveAndFlush(notif1);

        Thread.sleep(10);

        Notification notif2 = new Notification(3002L, 30L, NotificationType.ORDER_CREATED, "Second", NotificationStatus.UNREAD);
        notificationRepository.saveAndFlush(notif2);

        List<Notification> found = notificationRepository.findByCustomerIdOrderByCreatedAtDesc(30L);
        assertThat(found).hasSize(2);
        assertThat(found.get(0).getOrderId()).isEqualTo(3002L);
        assertThat(found.get(1).getOrderId()).isEqualTo(3001L);
    }

    @Test
    @DisplayName("existsByOrderIdAndType returns true when notification exists and false otherwise")
    void testExistsByOrderIdAndType() {
        Notification notif = new Notification(4001L, 40L, NotificationType.ORDER_CREATED, "Msg", NotificationStatus.UNREAD);
        notificationRepository.save(notif);

        assertThat(notificationRepository.existsByOrderIdAndType(4001L, NotificationType.ORDER_CREATED)).isTrue();
        assertThat(notificationRepository.existsByOrderIdAndType(9999L, NotificationType.ORDER_CREATED)).isFalse();
    }
}
