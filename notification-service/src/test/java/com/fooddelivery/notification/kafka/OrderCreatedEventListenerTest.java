package com.fooddelivery.notification.kafka;

import com.fooddelivery.common.event.OrderCreatedEvent;
import com.fooddelivery.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderCreatedEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private OrderCreatedEventListener listener;

    @Test
    @DisplayName("Listener receives OrderCreatedEvent and delegates to NotificationService")
    void testHandleOrderCreated() {
        OrderCreatedEvent event = new OrderCreatedEvent(
                100L, 5L, new BigDecimal("25.00"), "CONFIRMED", LocalDateTime.now()
        );

        listener.handleOrderCreated(event);

        verify(notificationService, times(1)).processOrderCreatedEvent(event);
    }

    @Test
    @DisplayName("Listener handles null event gracefully")
    void testHandleNullEvent() {
        listener.handleOrderCreated(null);

        verify(notificationService, never()).processOrderCreatedEvent(any());
    }
}
