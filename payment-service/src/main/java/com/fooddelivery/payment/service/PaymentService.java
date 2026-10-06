package com.fooddelivery.payment.service;

import com.fooddelivery.common.dto.PaymentRequest;
import com.fooddelivery.common.dto.PaymentResponse;
import com.fooddelivery.common.enums.PaymentStatus;

public interface PaymentService {

    PaymentResponse processPayment(PaymentRequest request);

    PaymentResponse getPaymentById(Long paymentId);

    PaymentResponse getPaymentByOrderId(Long orderId);

    PaymentResponse updatePaymentStatus(Long paymentId, PaymentStatus newStatus);
}
