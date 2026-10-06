package com.fooddelivery.food.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Should reject negative food price")
    void testNegativePriceRejected() {
        FoodItemCreateRequest request = new FoodItemCreateRequest(
                1L,
                "Burger",
                "Tasty burger",
                new BigDecimal("-5.00"),
                "Fast Food",
                true
        );

        Set<ConstraintViolation<FoodItemCreateRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("price")));
    }

    @Test
    @DisplayName("Should reject zero food price (below 0.01 minimum)")
    void testZeroPriceRejected() {
        FoodItemCreateRequest request = new FoodItemCreateRequest(
                1L,
                "Burger",
                "Tasty burger",
                BigDecimal.ZERO,
                "Fast Food",
                true
        );

        Set<ConstraintViolation<FoodItemCreateRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("price")));
    }

    @Test
    @DisplayName("Should reject blank food name")
    void testBlankFoodNameRejected() {
        FoodItemCreateRequest request = new FoodItemCreateRequest(
                1L,
                "   ",
                "Tasty burger",
                new BigDecimal("9.99"),
                "Fast Food",
                true
        );

        Set<ConstraintViolation<FoodItemCreateRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
    }

    @Test
    @DisplayName("Should reject missing restaurant ID")
    void testMissingRestaurantIdRejected() {
        FoodItemCreateRequest request = new FoodItemCreateRequest(
                null,
                "Burger",
                "Tasty burger",
                new BigDecimal("9.99"),
                "Fast Food",
                true
        );

        Set<ConstraintViolation<FoodItemCreateRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("restaurantId")));
    }

    @Test
    @DisplayName("Should reject blank restaurant name")
    void testBlankRestaurantNameRejected() {
        RestaurantCreateRequest request = new RestaurantCreateRequest(
                "  ",
                "123 Main St",
                "9876543210"
        );

        Set<ConstraintViolation<RestaurantCreateRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
    }
}
