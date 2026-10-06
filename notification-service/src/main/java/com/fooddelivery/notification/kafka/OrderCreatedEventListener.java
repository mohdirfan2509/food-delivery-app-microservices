package com.fooddelivery.notification.kafka;

import com.fooddelivery.common.event.OrderCreatedEvent;
import com.fooddelivery.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedEventListener.class);

    private final NotificationService notificationService;

    public OrderCreatedEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.order-created:order-created}",
            groupId = "${app.kafka.consumer-group:notification-service-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received order-created event for orderId={} and customerId={}",
                event != null ? event.getOrderId() : "null",
                event != null ? event.getCustomerId() : "null");

        if (event == null) {
            log.warn("Received null OrderCreatedEvent. Skipping.");
            return;
        }

        notificationService.processOrderCreatedEvent(event);
        log.info("Successfully processed order-created event for orderId={}", event.getOrderId());
    }
}
