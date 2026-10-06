package com.fooddelivery.order.repository;

import com.fooddelivery.order.entity.Order;
import com.fooddelivery.order.entity.OrderItem;
import com.fooddelivery.order.entity.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Test
    @DisplayName("Persist order with order items and verify cascade and relationships")
    void testPersistOrderWithItems() {
        Order order = new Order(100L, new BigDecimal("45.00"), OrderStatus.PENDING);
        OrderItem item1 = new OrderItem(1L, 10L, "Burger", 2, new BigDecimal("15.00"), new BigDecimal("30.00"));
        OrderItem item2 = new OrderItem(2L, 10L, "Fries", 1, new BigDecimal("15.00"), new BigDecimal("15.00"));
        order.addItem(item1);
        order.addItem(item2);

        Order saved = orderRepository.save(order);

        assertThat(saved.getOrderId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getItems()).hasSize(2);

        List<OrderItem> items = orderItemRepository.findAll();
        assertThat(items).hasSize(2);
        assertThat(items.get(0).getOrder().getOrderId()).isEqualTo(saved.getOrderId());
    }

    @Test
    @DisplayName("findByCustomerIdOrderByCreatedAtDesc retrieves orders ordered by creation time")
    void testFindByCustomerIdOrderByCreatedAtDesc() {
        Order order1 = new Order(200L, new BigDecimal("20.00"), OrderStatus.CONFIRMED);
        Order order2 = new Order(200L, new BigDecimal("50.00"), OrderStatus.CONFIRMED);
        orderRepository.save(order1);
        orderRepository.save(order2);

        List<Order> found = orderRepository.findByCustomerIdOrderByCreatedAtDesc(200L);
        assertThat(found).hasSize(2);
        assertThat(found.get(0).getCustomerId()).isEqualTo(200L);
    }
}
