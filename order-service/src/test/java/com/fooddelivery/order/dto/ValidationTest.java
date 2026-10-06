package com.fooddelivery.order.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Valid CreateOrderRequest has no violations")
    void testValidCreateOrderRequest() {
        OrderItemRequest item = new OrderItemRequest(1L, 2);
        CreateOrderRequest request = new CreateOrderRequest(List.of(item));

        Set<ConstraintViolation<CreateOrderRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("CreateOrderRequest with null or empty items fails validation")
    void testEmptyItemsValidation() {
        CreateOrderRequest nullItems = new CreateOrderRequest(null);
        Set<ConstraintViolation<CreateOrderRequest>> violations = validator.validate(nullItems);
        assertThat(violations).isNotEmpty();

        CreateOrderRequest emptyItems = new CreateOrderRequest(Collections.emptyList());
        violations = validator.validate(emptyItems);
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("OrderItemRequest with null foodId or invalid quantity fails validation")
    void testOrderItemValidation() {
        OrderItemRequest nullFoodId = new OrderItemRequest(null, 2);
        Set<ConstraintViolation<OrderItemRequest>> violations = validator.validate(nullFoodId);
        assertThat(violations).isNotEmpty();

        OrderItemRequest zeroQuantity = new OrderItemRequest(1L, 0);
        violations = validator.validate(zeroQuantity);
        assertThat(violations).isNotEmpty();

        OrderItemRequest negativeQuantity = new OrderItemRequest(1L, -5);
        violations = validator.validate(negativeQuantity);
        assertThat(violations).isNotEmpty();

        OrderItemRequest excessQuantity = new OrderItemRequest(1L, 101);
        violations = validator.validate(excessQuantity);
        assertThat(violations).isNotEmpty();
    }
}
