package com.fooddelivery.order.service;

import com.fooddelivery.common.dto.PaymentRequest;
import com.fooddelivery.common.dto.PaymentResponse;
import com.fooddelivery.common.enums.PaymentStatus;
import com.fooddelivery.common.event.OrderCreatedEvent;
import com.fooddelivery.order.client.FoodItemClientDto;
import com.fooddelivery.order.client.FoodServiceClient;
import com.fooddelivery.order.client.PaymentServiceClient;
import com.fooddelivery.order.dto.CreateOrderRequest;
import com.fooddelivery.order.dto.OrderItemRequest;
import com.fooddelivery.order.dto.OrderResponse;
import com.fooddelivery.order.entity.Order;
import com.fooddelivery.order.entity.OrderItem;
import com.fooddelivery.order.entity.OrderStatus;
import com.fooddelivery.order.exception.FoodNotFoundException;
import com.fooddelivery.order.exception.FoodUnavailableException;
import com.fooddelivery.order.exception.InvalidOrderException;
import com.fooddelivery.order.exception.OrderNotFoundException;
import com.fooddelivery.order.kafka.OrderEventProducer;
import com.fooddelivery.order.repository.OrderRepository;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private FoodServiceClient foodServiceClient;

    @Mock
    private PaymentServiceClient paymentServiceClient;

    @Mock
    private OrderEventProducer orderEventProducer;

    @InjectMocks
    private OrderServiceImpl orderService;

    private final String authHeader = "Bearer mock-jwt-token";

    @Test
    @DisplayName("Successful order creation with single item - calculates correct total, confirms order, and emits Kafka event")
    void testCreateOrderSingleItemSuccess() {
        Long customerId = 10L;
        CreateOrderRequest request = new CreateOrderRequest(List.of(new OrderItemRequest(101L, 2)));

        FoodItemClientDto food = new FoodItemClientDto(101L, 5L, "Pepperoni Pizza", new BigDecimal("250.00"), true);
        when(foodServiceClient.getFoodById(101L)).thenReturn(food);

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            if (o.getOrderId() == null) {
                o.setOrderId(1001L);
            }
            return o;
        });

        PaymentResponse paymentResponse = new PaymentResponse(501L, 1001L, new BigDecimal("500.00"), PaymentStatus.SUCCESS, LocalDateTime.now(), "TXN-12345");
        when(paymentServiceClient.processPayment(eq(authHeader), any(PaymentRequest.class))).thenReturn(paymentResponse);

        OrderResponse response = orderService.createOrder(customerId, authHeader, request);

        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo(1001L);
        assertThat(response.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getFoodName()).isEqualTo("Pepperoni Pizza");
        assertThat(response.getItems().get(0).getSubtotal()).isEqualByComparingTo(new BigDecimal("500.00"));

        verify(orderEventProducer, times(1)).publishOrderCreated(any(OrderCreatedEvent.class));
    }

    @Test
    @DisplayName("Successful order creation with multiple items - authoritative prices used and summed correctly")
    void testCreateOrderMultipleItemsSuccess() {
        Long customerId = 10L;
        CreateOrderRequest request = new CreateOrderRequest(List.of(
                new OrderItemRequest(101L, 2),
                new OrderItemRequest(102L, 3)
        ));

        FoodItemClientDto food1 = new FoodItemClientDto(101L, 5L, "Burger", new BigDecimal("150.00"), true);
        FoodItemClientDto food2 = new FoodItemClientDto(102L, 5L, "Fries", new BigDecimal("50.00"), true);

        when(foodServiceClient.getFoodById(101L)).thenReturn(food1);
        when(foodServiceClient.getFoodById(102L)).thenReturn(food2);

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            if (o.getOrderId() == null) {
                o.setOrderId(2002L);
            }
            return o;
        });

        PaymentResponse paymentResponse = new PaymentResponse(502L, 2002L, new BigDecimal("450.00"), PaymentStatus.SUCCESS, LocalDateTime.now(), "TXN-99999");
        when(paymentServiceClient.processPayment(eq(authHeader), any(PaymentRequest.class))).thenReturn(paymentResponse);

        OrderResponse response = orderService.createOrder(customerId, authHeader, request);

        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo(2002L);
        assertThat(response.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        // (150 * 2) + (50 * 3) = 300 + 150 = 450
        assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("450.00"));
        assertThat(response.getItems()).hasSize(2);

        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(orderEventProducer, times(1)).publishOrderCreated(eventCaptor.capture());
        OrderCreatedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getOrderId()).isEqualTo(2002L);
        assertThat(capturedEvent.getCustomerId()).isEqualTo(customerId);
        assertThat(capturedEvent.getTotalAmount()).isEqualByComparingTo(new BigDecimal("450.00"));
    }

    @Test
    @DisplayName("Food not found from Feign throws FoodNotFoundException, no order saved and no payment attempted")
    void testFoodNotFoundThrowsException() {
        Long customerId = 10L;
        CreateOrderRequest request = new CreateOrderRequest(List.of(new OrderItemRequest(999L, 1)));

        Request feignReq = Request.create(Request.HttpMethod.GET, "/foods/999", Collections.emptyMap(), null, new RequestTemplate());
        when(foodServiceClient.getFoodById(999L)).thenThrow(new FeignException.NotFound("Not found", feignReq, null, null));

        assertThatThrownBy(() -> orderService.createOrder(customerId, authHeader, request))
                .isInstanceOf(FoodNotFoundException.class)
                .hasMessageContaining("Food item not found with id: 999");

        verify(orderRepository, never()).save(any(Order.class));
        verify(paymentServiceClient, never()).processPayment(anyString(), any(PaymentRequest.class));
        verify(orderEventProducer, never()).publishOrderCreated(any());
    }

    @Test
    @DisplayName("Unavailable food throws FoodUnavailableException, no order saved and no payment attempted")
    void testFoodUnavailableThrowsException() {
        Long customerId = 10L;
        CreateOrderRequest request = new CreateOrderRequest(List.of(new OrderItemRequest(101L, 1)));

        FoodItemClientDto food = new FoodItemClientDto(101L, 5L, "Sold Out Pasta", new BigDecimal("200.00"), false);
        when(foodServiceClient.getFoodById(101L)).thenReturn(food);

        assertThatThrownBy(() -> orderService.createOrder(customerId, authHeader, request))
                .isInstanceOf(FoodUnavailableException.class)
                .hasMessageContaining("Food item is currently unavailable");

        verify(orderRepository, never()).save(any(Order.class));
        verify(paymentServiceClient, never()).processPayment(anyString(), any(PaymentRequest.class));
        verify(orderEventProducer, never()).publishOrderCreated(any());
    }

    @Test
    @DisplayName("Payment failure results in FAILED order and NO Kafka event published")
    void testPaymentFailureResultsInFailedOrder() {
        Long customerId = 10L;
        CreateOrderRequest request = new CreateOrderRequest(List.of(new OrderItemRequest(101L, 1)));

        FoodItemClientDto food = new FoodItemClientDto(101L, 5L, "Pizza", new BigDecimal("100.00"), true);
        when(foodServiceClient.getFoodById(101L)).thenReturn(food);

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            if (o.getOrderId() == null) {
                o.setOrderId(3003L);
            }
            return o;
        });

        PaymentResponse failedPayment = new PaymentResponse(503L, 3003L, new BigDecimal("100.00"), PaymentStatus.FAILED, LocalDateTime.now(), null);
        when(paymentServiceClient.processPayment(eq(authHeader), any(PaymentRequest.class))).thenReturn(failedPayment);

        OrderResponse response = orderService.createOrder(customerId, authHeader, request);

        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo(3003L);
        assertThat(response.getStatus()).isEqualTo(OrderStatus.FAILED);

        verify(orderEventProducer, never()).publishOrderCreated(any());
    }

    @Test
    @DisplayName("Payment client exception results in FAILED order and NO Kafka event published")
    void testPaymentClientExceptionResultsInFailedOrder() {
        Long customerId = 10L;
        CreateOrderRequest request = new CreateOrderRequest(List.of(new OrderItemRequest(101L, 1)));

        FoodItemClientDto food = new FoodItemClientDto(101L, 5L, "Pizza", new BigDecimal("100.00"), true);
        when(foodServiceClient.getFoodById(101L)).thenReturn(food);

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            if (o.getOrderId() == null) {
                o.setOrderId(4004L);
            }
            return o;
        });

        when(paymentServiceClient.processPayment(eq(authHeader), any(PaymentRequest.class)))
                .thenThrow(new RuntimeException("Payment gateway timeout"));

        OrderResponse response = orderService.createOrder(customerId, authHeader, request);

        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo(4004L);
        assertThat(response.getStatus()).isEqualTo(OrderStatus.FAILED);

        verify(orderEventProducer, never()).publishOrderCreated(any());
    }

    @Test
    @DisplayName("Invalid order request with null or empty items throws InvalidOrderException")
    void testEmptyItemsThrowsException() {
        assertThatThrownBy(() -> orderService.createOrder(1L, authHeader, null))
                .isInstanceOf(InvalidOrderException.class);

        assertThatThrownBy(() -> orderService.createOrder(1L, authHeader, new CreateOrderRequest(Collections.emptyList())))
                .isInstanceOf(InvalidOrderException.class);
    }

    @Test
    @DisplayName("Customer can view their own order")
    void testCustomerCanViewOwnOrder() {
        Order order = new Order(10L, new BigDecimal("100.00"), OrderStatus.CONFIRMED);
        order.setOrderId(1001L);
        when(orderRepository.findById(1001L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById(1001L, 10L, false);
        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo(1001L);
    }

    @Test
    @DisplayName("Customer cannot view another customer's order - throws AccessDeniedException")
    void testCustomerCannotViewOtherOrder() {
        Order order = new Order(20L, new BigDecimal("100.00"), OrderStatus.CONFIRMED);
        order.setOrderId(1001L);
        when(orderRepository.findById(1001L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.getOrderById(1001L, 10L, false))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You are not authorized to view this order");
    }

    @Test
    @DisplayName("Admin can view any customer's order")
    void testAdminCanViewAnyOrder() {
        Order order = new Order(20L, new BigDecimal("100.00"), OrderStatus.CONFIRMED);
        order.setOrderId(1001L);
        when(orderRepository.findById(1001L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById(1001L, 999L, true);
        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo(1001L);
    }

    @Test
    @DisplayName("Order not found throws OrderNotFoundException")
    void testOrderNotFoundThrowsException() {
        when(orderRepository.findById(9999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(9999L, 10L, false))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("Order not found with id: 9999");
    }

    @Test
    @DisplayName("getOrdersByCustomerId returns only orders belonging to customer")
    void testGetOrdersByCustomerId() {
        Order order1 = new Order(10L, new BigDecimal("50.00"), OrderStatus.CONFIRMED);
        order1.setOrderId(1L);
        Order order2 = new Order(10L, new BigDecimal("75.00"), OrderStatus.CONFIRMED);
        order2.setOrderId(2L);

        when(orderRepository.findByCustomerIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(order2, order1));

        List<OrderResponse> orders = orderService.getOrdersByCustomerId(10L);
        assertThat(orders).hasSize(2);
        assertThat(orders.get(0).getOrderId()).isEqualTo(2L);
        assertThat(orders.get(1).getOrderId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getAllOrders returns all orders for admin")
    void testGetAllOrders() {
        Order order1 = new Order(10L, new BigDecimal("50.00"), OrderStatus.CONFIRMED);
        order1.setOrderId(1L);
        Order order2 = new Order(20L, new BigDecimal("75.00"), OrderStatus.CONFIRMED);
        order2.setOrderId(2L);

        when(orderRepository.findAll()).thenReturn(List.of(order1, order2));

        List<OrderResponse> orders = orderService.getAllOrders();
        assertThat(orders).hasSize(2);
    }
}
