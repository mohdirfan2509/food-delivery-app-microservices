package com.fooddelivery.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.common.dto.PaymentRequest;
import com.fooddelivery.common.dto.PaymentResponse;
import com.fooddelivery.common.enums.PaymentStatus;
import com.fooddelivery.common.event.OrderCreatedEvent;
import com.fooddelivery.order.client.FoodItemClientDto;
import com.fooddelivery.order.client.FoodServiceClient;
import com.fooddelivery.order.client.PaymentServiceClient;
import com.fooddelivery.order.dto.CreateOrderRequest;
import com.fooddelivery.order.dto.OrderItemRequest;
import com.fooddelivery.order.entity.Order;
import com.fooddelivery.order.entity.OrderItem;
import com.fooddelivery.order.entity.OrderStatus;
import com.fooddelivery.order.kafka.OrderEventProducer;
import com.fooddelivery.order.repository.OrderRepository;
import com.fooddelivery.order.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FoodServiceClient foodServiceClient;

    @MockBean
    private PaymentServiceClient paymentServiceClient;

    @MockBean
    private OrderEventProducer orderEventProducer;

    private String customer1Token;
    private String customer2Token;
    private String adminToken;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        customer1Token = "Bearer " + jwtTokenProvider.generateToken(1L, "cust1@foodapp.com", "CUSTOMER");
        customer2Token = "Bearer " + jwtTokenProvider.generateToken(2L, "cust2@foodapp.com", "CUSTOMER");
        adminToken = "Bearer " + jwtTokenProvider.generateToken(999L, "admin@foodapp.com", "ADMIN");
    }

    @Test
    @DisplayName("POST /orders - Unauthenticated request returns 401 Unauthorized")
    void testCreateOrderUnauthenticated() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(List.of(new OrderItemRequest(1L, 2)));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("POST /orders - Empty items list returns 400 Bad Request")
    void testCreateOrderEmptyItems() throws Exception {
        String payload = "{\"items\": []}";

        mockMvc.perform(post("/orders")
                        .header(HttpHeaders.AUTHORIZATION, customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("POST /orders - Non-positive quantity returns 400 Bad Request")
    void testCreateOrderInvalidQuantity() throws Exception {
        String payload = "{\"items\": [{\"foodId\": 1, \"quantity\": 0}]}";

        mockMvc.perform(post("/orders")
                        .header(HttpHeaders.AUTHORIZATION, customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("POST /orders - Price manipulation attempt is ignored; authoritative price is used")
    void testCreateOrderPriceManipulationPrevented() throws Exception {
        // Client attempts to send manipulated price (0.01) and manipulated total (0.02)
        String maliciousPayload = "{\n" +
                "  \"items\": [\n" +
                "    {\n" +
                "      \"foodId\": 10,\n" +
                "      \"quantity\": 2,\n" +
                "      \"price\": 0.01\n" +
                "    }\n" +
                "  ],\n" +
                "  \"totalAmount\": 0.02\n" +
                "}";

        FoodItemClientDto authoritativeFood = new FoodItemClientDto(
                10L, 2L, "Special Biryani", new BigDecimal("250.00"), true
        );
        when(foodServiceClient.getFoodById(10L)).thenReturn(authoritativeFood);

        PaymentResponse paymentResponse = new PaymentResponse(
                101L, 1L, new BigDecimal("500.00"), PaymentStatus.SUCCESS, LocalDateTime.now(), "TXN-TEST-1"
        );
        when(paymentServiceClient.processPayment(anyString(), any(PaymentRequest.class))).thenReturn(paymentResponse);

        mockMvc.perform(post("/orders")
                        .header(HttpHeaders.AUTHORIZATION, customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(maliciousPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(1))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                // Total must be 250.00 * 2 = 500.00, NEVER 0.02
                .andExpect(jsonPath("$.totalAmount").value(500.00))
                .andExpect(jsonPath("$.items[0].foodId").value(10))
                .andExpect(jsonPath("$.items[0].foodName").value("Special Biryani"))
                .andExpect(jsonPath("$.items[0].unitPrice").value(250.00))
                .andExpect(jsonPath("$.items[0].subtotal").value(500.00));

        // Verify Kafka event published with correct amount
        verify(orderEventProducer, times(1)).publishOrderCreated(any(OrderCreatedEvent.class));

        // Verify persisted order in DB
        List<Order> orders = orderRepository.findAll();
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getTotalAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(orders.get(0).getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("GET /orders/{orderId} - Customer can access own order")
    void testCustomerCanAccessOwnOrder() throws Exception {
        Order order = new Order(1L, new BigDecimal("100.00"), OrderStatus.CONFIRMED);
        OrderItem item = new OrderItem(5L, 2L, "Noodles", 1, new BigDecimal("100.00"), new BigDecimal("100.00"));
        order.addItem(item);
        Order saved = orderRepository.save(order);

        mockMvc.perform(get("/orders/" + saved.getOrderId())
                        .header(HttpHeaders.AUTHORIZATION, customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(saved.getOrderId()))
                .andExpect(jsonPath("$.customerId").value(1));
    }

    @Test
    @DisplayName("GET /orders/{orderId} - Customer cannot access another customer's order (403 Forbidden)")
    void testCustomerCannotAccessAnotherCustomerOrder() throws Exception {
        Order order = new Order(1L, new BigDecimal("100.00"), OrderStatus.CONFIRMED);
        Order saved = orderRepository.save(order);

        mockMvc.perform(get("/orders/" + saved.getOrderId())
                        .header(HttpHeaders.AUTHORIZATION, customer2Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("GET /orders/{orderId} - Admin can access any order")
    void testAdminCanAccessAnyOrder() throws Exception {
        Order order = new Order(1L, new BigDecimal("100.00"), OrderStatus.CONFIRMED);
        Order saved = orderRepository.save(order);

        mockMvc.perform(get("/orders/" + saved.getOrderId())
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(saved.getOrderId()))
                .andExpect(jsonPath("$.customerId").value(1));
    }

    @Test
    @DisplayName("GET /orders/my-orders - Returns orders belonging to authenticated customer")
    void testGetMyOrders() throws Exception {
        Order order1 = new Order(1L, new BigDecimal("80.00"), OrderStatus.CONFIRMED);
        Order order2 = new Order(2L, new BigDecimal("120.00"), OrderStatus.CONFIRMED);
        orderRepository.save(order1);
        orderRepository.save(order2);

        mockMvc.perform(get("/orders/my-orders")
                        .header(HttpHeaders.AUTHORIZATION, customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].customerId").value(1));
    }

    @Test
    @DisplayName("GET /orders - Admin can view all orders, Customer gets 403 Forbidden")
    void testGetAllOrdersAuthorization() throws Exception {
        Order order = new Order(1L, new BigDecimal("80.00"), OrderStatus.CONFIRMED);
        orderRepository.save(order);

        // Admin: allowed (200 OK)
        mockMvc.perform(get("/orders")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // Customer: forbidden (403 Forbidden)
        mockMvc.perform(get("/orders")
                        .header(HttpHeaders.AUTHORIZATION, customer1Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }
}
