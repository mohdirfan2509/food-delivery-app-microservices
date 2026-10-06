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
import com.fooddelivery.order.dto.OrderItemResponse;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final FoodServiceClient foodServiceClient;
    private final PaymentServiceClient paymentServiceClient;
    private final OrderEventProducer orderEventProducer;

    public OrderServiceImpl(OrderRepository orderRepository,
                            FoodServiceClient foodServiceClient,
                            PaymentServiceClient paymentServiceClient,
                            OrderEventProducer orderEventProducer) {
        this.orderRepository = orderRepository;
        this.foodServiceClient = foodServiceClient;
        this.paymentServiceClient = paymentServiceClient;
        this.orderEventProducer = orderEventProducer;
    }

    @Override
    public OrderResponse createOrder(Long customerId, String authorizationToken, CreateOrderRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new InvalidOrderException("Order must contain at least one item");
        }

        log.info("Initiating order creation for customerId: {} with {} items", customerId, request.getItems().size());

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        // 1. Authoritative Food Validation & Price Calculation via Food Service
        for (OrderItemRequest itemReq : request.getItems()) {
            if (itemReq.getQuantity() == null || itemReq.getQuantity() <= 0) {
                throw new InvalidOrderException("Quantity must be greater than zero for foodId: " + itemReq.getFoodId());
            }

            FoodItemClientDto food;
            try {
                food = foodServiceClient.getFoodById(itemReq.getFoodId());
            } catch (FeignException.NotFound ex) {
                throw new FoodNotFoundException("Food item not found with id: " + itemReq.getFoodId());
            } catch (FeignException ex) {
                log.error("Failed to fetch food details for foodId {}: status {}", itemReq.getFoodId(), ex.status());
                if (ex.status() == 404) {
                    throw new FoodNotFoundException("Food item not found with id: " + itemReq.getFoodId());
                }
                throw ex;
            }

            if (food == null) {
                throw new FoodNotFoundException("Food item not found with id: " + itemReq.getFoodId());
            }

            if (food.getAvailable() == null || !food.getAvailable()) {
                log.warn("Food item {} is unavailable", food.getName());
                throw new FoodUnavailableException("Food item is currently unavailable: " + food.getName() + " (ID: " + itemReq.getFoodId() + ")");
            }

            BigDecimal unitPrice = food.getPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            OrderItem orderItem = new OrderItem(
                    food.getFoodId(),
                    food.getRestaurantId(),
                    food.getName(),
                    itemReq.getQuantity(),
                    unitPrice,
                    subtotal
            );
            orderItems.add(orderItem);
        }

        // 2. Persist initial order in PENDING state to acquire unique orderId
        Order order = new Order(customerId, totalAmount, OrderStatus.PENDING);
        for (OrderItem item : orderItems) {
            order.addItem(item);
        }
        Order savedOrder = saveOrder(order);
        log.info("Saved initial PENDING order with ID: {}", savedOrder.getOrderId());

        // 3. Process Payment via Payment Service OpenFeign Client
        boolean paymentSuccess = false;
        try {
            PaymentRequest paymentRequest = new PaymentRequest(savedOrder.getOrderId(), customerId, savedOrder.getTotalAmount());
            PaymentResponse paymentResponse = paymentServiceClient.processPayment(authorizationToken, paymentRequest);

            if (paymentResponse != null && paymentResponse.getPaymentStatus() == PaymentStatus.SUCCESS) {
                paymentSuccess = true;
            } else {
                log.warn("Payment service returned non-success status for order {}: {}",
                        savedOrder.getOrderId(), paymentResponse != null ? paymentResponse.getPaymentStatus() : "null");
            }
        } catch (Exception ex) {
            log.error("Payment execution error for order {}: {}", savedOrder.getOrderId(), ex.getMessage());
            paymentSuccess = false;
        }

        // 4. Update Order Status
        if (paymentSuccess) {
            savedOrder.setStatus(OrderStatus.CONFIRMED);
            savedOrder = saveOrder(savedOrder);
            log.info("Order ID {} confirmed successfully", savedOrder.getOrderId());

            // 5. Publish Kafka Event
            OrderCreatedEvent event = new OrderCreatedEvent(
                    savedOrder.getOrderId(),
                    savedOrder.getCustomerId(),
                    savedOrder.getTotalAmount(),
                    savedOrder.getStatus().name(),
                    LocalDateTime.now()
            );
            orderEventProducer.publishOrderCreated(event);
        } else {
            savedOrder.setStatus(OrderStatus.FAILED);
            savedOrder = saveOrder(savedOrder);
            log.warn("Order ID {} marked as FAILED due to payment issue", savedOrder.getOrderId());
        }

        return mapToResponse(savedOrder);
    }

    @Transactional
    public Order saveOrder(Order order) {
        return orderRepository.save(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId, Long currentUserId, boolean isAdmin) {
        log.info("Fetching order by ID: {} for user: {} (isAdmin: {})", orderId, currentUserId, isAdmin);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));

        if (!isAdmin && !order.getCustomerId().equals(currentUserId)) {
            log.warn("Access denied: user {} attempted to access order {} owned by {}",
                    currentUserId, orderId, order.getCustomerId());
            throw new AccessDeniedException("You are not authorized to view this order");
        }

        return mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomerId(Long customerId) {
        log.info("Fetching orders for customer: {}", customerId);
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        log.info("Admin fetching all orders");
        return orderRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getOrderItemId(),
                        item.getFoodId(),
                        item.getRestaurantId(),
                        item.getFoodName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSubtotal()
                ))
                .toList();

        return new OrderResponse(
                order.getOrderId(),
                order.getCustomerId(),
                order.getTotalAmount(),
                order.getStatus(),
                itemResponses,
                order.getCreatedAt() != null ? order.getCreatedAt() : LocalDateTime.now(),
                order.getUpdatedAt() != null ? order.getUpdatedAt() : LocalDateTime.now()
        );
    }
}
