package com.fooddelivery.order.kafka;

import com.fooddelivery.common.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventProducer.class);

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    @Value("${app.kafka.topics.order-created:order-created}")
    private String orderCreatedTopic;

    public OrderEventProducer(KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishOrderCreated(OrderCreatedEvent event) {
        log.info("Publishing OrderCreatedEvent to topic '{}': orderId={}, customerId={}, amount={}",
                orderCreatedTopic, event.getOrderId(), event.getCustomerId(), event.getTotalAmount());
        kafkaTemplate.send(orderCreatedTopic, String.valueOf(event.getOrderId()), event);
    }
}
