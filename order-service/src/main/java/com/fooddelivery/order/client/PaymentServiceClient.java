package com.fooddelivery.order.client;

import com.fooddelivery.common.dto.PaymentRequest;
import com.fooddelivery.common.dto.PaymentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "payment-service", url = "${services.payment.url:http://localhost:8084}")
public interface PaymentServiceClient {

    @PostMapping("/payments")
    PaymentResponse processPayment(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationToken,
            @RequestBody PaymentRequest request);
}
