package com.fooddelivery.payment.service;

import com.fooddelivery.common.dto.PaymentRequest;
import com.fooddelivery.common.dto.PaymentResponse;
import com.fooddelivery.common.enums.PaymentStatus;
import com.fooddelivery.payment.entity.Payment;
import com.fooddelivery.payment.exception.InvalidPaymentException;
import com.fooddelivery.payment.exception.InvalidStateTransitionException;
import com.fooddelivery.payment.exception.PaymentNotFoundException;
import com.fooddelivery.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository paymentRepository;

    public PaymentServiceImpl(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        if (request == null) {
            throw new InvalidPaymentException("Payment request cannot be null");
        }
        if (request.getOrderId() == null) {
            throw new InvalidPaymentException("Order ID is required");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPaymentException("Payment amount must be greater than zero");
        }

        log.info("Processing payment for orderId: {}, amount: {}", request.getOrderId(), request.getAmount());

        // 1. Idempotency Check
        Optional<Payment> existingOpt = paymentRepository.findByOrderId(request.getOrderId());
        if (existingOpt.isPresent()) {
            Payment existing = existingOpt.get();
            log.info("Existing payment found for orderId: {} with status: {}. Returning idempotent response.",
                    request.getOrderId(), existing.getStatus());
            return mapToResponse(existing);
        }

        // 2. New Payment Processing (Simulated)
        String transactionReference = "TXN-" + UUID.randomUUID().toString();
        Payment payment = new Payment(
                request.getOrderId(),
                request.getAmount(),
                PaymentStatus.SUCCESS,
                transactionReference
        );

        try {
            Payment saved = paymentRepository.save(payment);
            log.info("Successfully created and processed payment ID: {} with txnRef: {}",
                    saved.getPaymentId(), saved.getTransactionReference());
            return mapToResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Concurrent insert conflict detected for orderId: {}. Retrieving existing payment.",
                    request.getOrderId());
            return paymentRepository.findByOrderId(request.getOrderId())
                    .map(this::mapToResponse)
                    .orElseThrow(() -> ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long paymentId) {
        log.info("Fetching payment by ID: {}", paymentId);
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with id: " + paymentId));
        return mapToResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(Long orderId) {
        log.info("Fetching payment by order ID: {}", orderId);
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for order id: " + orderId));
        return mapToResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse updatePaymentStatus(Long paymentId, PaymentStatus newStatus) {
        log.info("Updating status of payment ID: {} to {}", paymentId, newStatus);
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with id: " + paymentId));

        PaymentStatus currentStatus = payment.getStatus();

        // Validate state transitions
        if (currentStatus == PaymentStatus.SUCCESS && newStatus != PaymentStatus.SUCCESS) {
            throw new InvalidStateTransitionException(
                    "Cannot transition payment status from " + currentStatus + " to " + newStatus);
        }
        if (currentStatus == PaymentStatus.FAILED && newStatus != PaymentStatus.FAILED) {
            throw new InvalidStateTransitionException(
                    "Cannot transition payment status from " + currentStatus + " to " + newStatus);
        }

        payment.setStatus(newStatus);
        Payment updated = paymentRepository.save(payment);
        return mapToResponse(updated);
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return new PaymentResponse(
                payment.getPaymentId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getCreatedAt() != null ? payment.getCreatedAt() : LocalDateTime.now(),
                payment.getTransactionReference()
        );
    }
}
