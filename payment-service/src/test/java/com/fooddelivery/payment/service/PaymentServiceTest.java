package com.fooddelivery.payment.service;

import com.fooddelivery.common.dto.PaymentRequest;
import com.fooddelivery.common.dto.PaymentResponse;
import com.fooddelivery.common.enums.PaymentStatus;
import com.fooddelivery.payment.entity.Payment;
import com.fooddelivery.payment.exception.InvalidPaymentException;
import com.fooddelivery.payment.exception.InvalidStateTransitionException;
import com.fooddelivery.payment.exception.PaymentNotFoundException;
import com.fooddelivery.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Payment payment;

    @BeforeEach
    void setUp() {
        payment = new Payment(
                1001L,
                new BigDecimal("25.50"),
                PaymentStatus.SUCCESS,
                "TXN-12345-ABCDE"
        );
        payment.setPaymentId(10L);
    }

    @Test
    @DisplayName("Should successfully process new payment")
    void testProcessPaymentSuccess() {
        PaymentRequest request = new PaymentRequest(1001L, 2001L, new BigDecimal("25.50"));

        when(paymentRepository.findByOrderId(1001L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        PaymentResponse response = paymentService.processPayment(request);

        assertNotNull(response);
        assertEquals(10L, response.getPaymentId());
        assertEquals(1001L, response.getOrderId());
        assertEquals(new BigDecimal("25.50"), response.getAmount());
        assertEquals(PaymentStatus.SUCCESS, response.getPaymentStatus());
        assertEquals("TXN-12345-ABCDE", response.getTransactionReference());
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("Should return existing payment when duplicate order request is received (Idempotency)")
    void testProcessPaymentIdempotent() {
        PaymentRequest request = new PaymentRequest(1001L, new BigDecimal("25.50"));

        when(paymentRepository.findByOrderId(1001L)).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.processPayment(request);

        assertNotNull(response);
        assertEquals(10L, response.getPaymentId());
        assertEquals(1001L, response.getOrderId());
        assertEquals(PaymentStatus.SUCCESS, response.getPaymentStatus());
        // Verify save was never called because existing payment was returned
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Should reject payment request with null or zero amount")
    void testProcessPaymentInvalidAmount() {
        PaymentRequest zeroRequest = new PaymentRequest(1001L, BigDecimal.ZERO);
        assertThrows(InvalidPaymentException.class, () -> paymentService.processPayment(zeroRequest));

        PaymentRequest negativeRequest = new PaymentRequest(1001L, new BigDecimal("-10.00"));
        assertThrows(InvalidPaymentException.class, () -> paymentService.processPayment(negativeRequest));

        PaymentRequest nullAmountRequest = new PaymentRequest(1001L, null);
        assertThrows(InvalidPaymentException.class, () -> paymentService.processPayment(nullAmountRequest));
    }

    @Test
    @DisplayName("Should reject payment request with null order ID")
    void testProcessPaymentNullOrderId() {
        PaymentRequest request = new PaymentRequest(null, new BigDecimal("25.50"));
        assertThrows(InvalidPaymentException.class, () -> paymentService.processPayment(request));
    }

    @Test
    @DisplayName("Should get payment by payment ID")
    void testGetPaymentById() {
        when(paymentRepository.findById(10L)).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.getPaymentById(10L);

        assertNotNull(response);
        assertEquals(10L, response.getPaymentId());
        assertEquals(1001L, response.getOrderId());
    }

    @Test
    @DisplayName("Should throw PaymentNotFoundException when payment ID does not exist")
    void testGetPaymentByIdNotFound() {
        when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> paymentService.getPaymentById(999L));
    }

    @Test
    @DisplayName("Should get payment by order ID")
    void testGetPaymentByOrderId() {
        when(paymentRepository.findByOrderId(1001L)).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.getPaymentByOrderId(1001L);

        assertNotNull(response);
        assertEquals(1001L, response.getOrderId());
    }

    @Test
    @DisplayName("Should throw PaymentNotFoundException when order ID does not exist")
    void testGetPaymentByOrderIdNotFound() {
        when(paymentRepository.findByOrderId(999L)).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> paymentService.getPaymentByOrderId(999L));
    }

    @Test
    @DisplayName("Should reject invalid state transition: SUCCESS to FAILED")
    void testInvalidStateTransitionSuccessToFailed() {
        when(paymentRepository.findById(10L)).thenReturn(Optional.of(payment));

        InvalidStateTransitionException ex = assertThrows(InvalidStateTransitionException.class,
                () -> paymentService.updatePaymentStatus(10L, PaymentStatus.FAILED));

        assertTrue(ex.getMessage().contains("Cannot transition payment status from SUCCESS to FAILED"));
    }

    @Test
    @DisplayName("Should reject invalid state transition: SUCCESS to PENDING")
    void testInvalidStateTransitionSuccessToPending() {
        when(paymentRepository.findById(10L)).thenReturn(Optional.of(payment));

        InvalidStateTransitionException ex = assertThrows(InvalidStateTransitionException.class,
                () -> paymentService.updatePaymentStatus(10L, PaymentStatus.PENDING));

        assertTrue(ex.getMessage().contains("Cannot transition payment status from SUCCESS to PENDING"));
    }
}
