package com.fooddelivery.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.common.dto.PaymentRequest;
import com.fooddelivery.common.dto.PaymentResponse;
import com.fooddelivery.payment.entity.Payment;
import com.fooddelivery.payment.repository.PaymentRepository;
import com.fooddelivery.payment.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private String validToken;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        validToken = tokenProvider.generateToken(1L, "customer@foodapp.com", "CUSTOMER");
    }

    @Test
    @DisplayName("POST /payments - Unauthenticated request returns 401 Unauthorized")
    void testProcessPaymentUnauthenticated() throws Exception {
        PaymentRequest request = new PaymentRequest(1001L, new BigDecimal("25.50"));

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("POST /payments - Malformed token returns 401 Unauthorized")
    void testProcessPaymentMalformedToken() throws Exception {
        PaymentRequest request = new PaymentRequest(1001L, new BigDecimal("25.50"));

        mockMvc.perform(post("/payments")
                        .header("Authorization", "Bearer invalid-tampered-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /payments - Authenticated valid payment returns 200 OK with SUCCESS status")
    void testProcessPaymentAuthenticatedSuccess() throws Exception {
        PaymentRequest request = new PaymentRequest(1001L, new BigDecimal("25.50"));

        mockMvc.perform(post("/payments")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1001))
                .andExpect(jsonPath("$.amount").value(25.50))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.paymentId").isNumber())
                .andExpect(jsonPath("$.transactionReference").isString());

        assertThat(paymentRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("POST /payments - Idempotency test: duplicate payment request returns existing payment and creates NO duplicate row")
    void testPaymentIdempotencyIntegration() throws Exception {
        PaymentRequest request = new PaymentRequest(1001L, new BigDecimal("25.50"));

        // First Request
        MvcResult firstResult = mockMvc.perform(post("/payments")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        PaymentResponse firstResponse = objectMapper.readValue(
                firstResult.getResponse().getContentAsString(), PaymentResponse.class);

        // Second Request (Duplicate for same order)
        MvcResult secondResult = mockMvc.perform(post("/payments")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        PaymentResponse secondResponse = objectMapper.readValue(
                secondResult.getResponse().getContentAsString(), PaymentResponse.class);

        // Verify identical response properties
        assertThat(secondResponse.getPaymentId()).isEqualTo(firstResponse.getPaymentId());
        assertThat(secondResponse.getOrderId()).isEqualTo(firstResponse.getOrderId());
        assertThat(secondResponse.getTransactionReference()).isEqualTo(firstResponse.getTransactionReference());
        assertThat(secondResponse.getPaymentStatus()).isEqualTo(firstResponse.getPaymentStatus());

        // Verify database still contains EXACTLY ONE record for order 1001
        assertThat(paymentRepository.count()).isEqualTo(1);
        assertThat(paymentRepository.findByOrderId(1001L)).isPresent();
    }

    @Test
    @DisplayName("Database uniqueness constraint enforces single payment per orderId")
    void testDatabaseUniquenessConstraint() {
        Payment payment1 = new Payment(2001L, new BigDecimal("10.00"), com.fooddelivery.common.enums.PaymentStatus.SUCCESS, "TXN-1");
        paymentRepository.saveAndFlush(payment1);

        Payment payment2 = new Payment(2001L, new BigDecimal("10.00"), com.fooddelivery.common.enums.PaymentStatus.SUCCESS, "TXN-2");
        assertThrows(DataIntegrityViolationException.class, () -> paymentRepository.saveAndFlush(payment2));
    }

    @Test
    @DisplayName("GET /payments/{paymentId} - Lookup by payment ID")
    void testGetPaymentById() throws Exception {
        PaymentRequest request = new PaymentRequest(3001L, new BigDecimal("45.00"));

        MvcResult result = mockMvc.perform(post("/payments")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        PaymentResponse created = objectMapper.readValue(result.getResponse().getContentAsString(), PaymentResponse.class);

        mockMvc.perform(get("/payments/" + created.getPaymentId())
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(created.getPaymentId()))
                .andExpect(jsonPath("$.orderId").value(3001))
                .andExpect(jsonPath("$.amount").value(45.00));
    }

    @Test
    @DisplayName("GET /payments/{paymentId} - Nonexistent ID returns 404 Not Found")
    void testGetPaymentByIdNotFound() throws Exception {
        mockMvc.perform(get("/payments/999999")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /payments/order/{orderId} - Lookup by order ID")
    void testGetPaymentByOrderId() throws Exception {
        PaymentRequest request = new PaymentRequest(4001L, new BigDecimal("55.00"));

        mockMvc.perform(post("/payments")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/payments/order/4001")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(4001))
                .andExpect(jsonPath("$.amount").value(55.00))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("GET /payments/order/{orderId} - Nonexistent order returns 404 Not Found")
    void testGetPaymentByOrderIdNotFound() throws Exception {
        mockMvc.perform(get("/payments/order/888888")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }
}
