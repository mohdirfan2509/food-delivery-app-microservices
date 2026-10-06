package com.fooddelivery.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fooddelivery.common.dto.PaymentRequest;
import com.fooddelivery.common.dto.PaymentResponse;
import com.fooddelivery.common.enums.PaymentStatus;
import com.fooddelivery.common.event.OrderCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DtoSerializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Should serialize and deserialize PaymentRequest correctly")
    void testPaymentRequestSerialization() throws Exception {
        PaymentRequest request = new PaymentRequest(101L, 501L, new BigDecimal("49.99"));

        String json = objectMapper.writeValueAsString(request);
        PaymentRequest deserialized = objectMapper.readValue(json, PaymentRequest.class);

        assertThat(deserialized).isEqualTo(request);
        assertThat(deserialized.getOrderId()).isEqualTo(101L);
        assertThat(deserialized.getCustomerId()).isEqualTo(501L);
        assertThat(deserialized.getAmount()).isEqualByComparingTo(new BigDecimal("49.99"));
    }

    @Test
    @DisplayName("Should serialize and deserialize PaymentResponse correctly")
    void testPaymentResponseSerialization() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 10, 6, 12, 0, 0);
        PaymentResponse response = new PaymentResponse(
                901L,
                101L,
                new BigDecimal("49.99"),
                PaymentStatus.SUCCESS,
                now,
                "TXN-901-ABC"
        );

        String json = objectMapper.writeValueAsString(response);
        PaymentResponse deserialized = objectMapper.readValue(json, PaymentResponse.class);

        assertThat(deserialized).isEqualTo(response);
        assertThat(deserialized.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(deserialized.getTransactionReference()).isEqualTo("TXN-901-ABC");
    }

    @Test
    @DisplayName("Should serialize and deserialize OrderCreatedEvent correctly")
    void testOrderCreatedEventSerialization() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 10, 6, 12, 0, 0);
        OrderCreatedEvent event = new OrderCreatedEvent(
                101L,
                501L,
                new BigDecimal("49.99"),
                "CREATED",
                now
        );

        String json = objectMapper.writeValueAsString(event);
        OrderCreatedEvent deserialized = objectMapper.readValue(json, OrderCreatedEvent.class);

        assertThat(deserialized).isEqualTo(event);
        assertThat(deserialized.getOrderId()).isEqualTo(101L);
        assertThat(deserialized.getCustomerId()).isEqualTo(501L);
        assertThat(deserialized.getTotalAmount()).isEqualByComparingTo(new BigDecimal("49.99"));
        assertThat(deserialized.getStatus()).isEqualTo("CREATED");
    }
}
