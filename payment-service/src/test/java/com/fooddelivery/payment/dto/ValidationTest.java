package com.fooddelivery.payment.dto;

import com.fooddelivery.common.dto.PaymentRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Should reject null orderId")
    void testNullOrderIdRejected() {
        PaymentRequest request = new PaymentRequest(null, new BigDecimal("25.50"));

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("orderId")));
    }

    @Test
    @DisplayName("Should reject null amount")
    void testNullAmountRejected() {
        PaymentRequest request = new PaymentRequest(1001L, null);

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("amount")));
    }

    @Test
    @DisplayName("Should reject zero amount")
    void testZeroAmountRejected() {
        PaymentRequest request = new PaymentRequest(1001L, BigDecimal.ZERO);

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("amount")));
    }

    @Test
    @DisplayName("Should reject negative amount")
    void testNegativeAmountRejected() {
        PaymentRequest request = new PaymentRequest(1001L, new BigDecimal("-15.00"));

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("amount")));
    }

    @Test
    @DisplayName("Should accept valid PaymentRequest with or without customerId")
    void testValidPaymentRequestAccepted() {
        PaymentRequest requestWithoutCustomer = new PaymentRequest(1001L, new BigDecimal("25.50"));
        assertTrue(validator.validate(requestWithoutCustomer).isEmpty());

        PaymentRequest requestWithCustomer = new PaymentRequest(1001L, 501L, new BigDecimal("25.50"));
        assertTrue(validator.validate(requestWithCustomer).isEmpty());
    }
}
