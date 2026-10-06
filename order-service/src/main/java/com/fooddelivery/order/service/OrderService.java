package com.fooddelivery.order.service;

import com.fooddelivery.order.dto.CreateOrderRequest;
import com.fooddelivery.order.dto.OrderResponse;

import java.util.List;

public interface OrderService {

    OrderResponse createOrder(Long customerId, String authorizationToken, CreateOrderRequest request);

    OrderResponse getOrderById(Long orderId, Long currentUserId, boolean isAdmin);

    List<OrderResponse> getOrdersByCustomerId(Long customerId);

    List<OrderResponse> getAllOrders();
}
