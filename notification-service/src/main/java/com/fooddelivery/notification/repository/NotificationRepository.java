package com.fooddelivery.notification.repository;

import com.fooddelivery.notification.entity.Notification;
import com.fooddelivery.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    boolean existsByOrderIdAndType(Long orderId, NotificationType type);

    Optional<Notification> findByOrderIdAndType(Long orderId, NotificationType type);
}
