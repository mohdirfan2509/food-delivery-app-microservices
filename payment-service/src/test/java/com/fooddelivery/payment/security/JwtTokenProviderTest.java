package com.fooddelivery.payment.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private final String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(secret, 3600000L);
    }

    @Test
    @DisplayName("Should generate and validate valid JWT token")
    void testValidToken() {
        String token = jwtTokenProvider.generateToken(1L, "customer@foodapp.com", "CUSTOMER");

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals("customer@foodapp.com", jwtTokenProvider.extractEmail(token));
        assertEquals("CUSTOMER", jwtTokenProvider.extractRole(token));
        assertEquals(1L, jwtTokenProvider.extractCustomerId(token));
    }

    @Test
    @DisplayName("Should reject tampered JWT token")
    void testTamperedToken() {
        String token = jwtTokenProvider.generateToken(1L, "customer@foodapp.com", "CUSTOMER");
        String tampered = token + "invalid";

        assertFalse(jwtTokenProvider.validateToken(tampered));
    }

    @Test
    @DisplayName("Should reject expired JWT token")
    void testExpiredToken() {
        JwtTokenProvider expiredProvider = new JwtTokenProvider(secret, -5000L);
        String token = expiredProvider.generateToken(1L, "customer@foodapp.com", "CUSTOMER");

        assertFalse(jwtTokenProvider.validateToken(token));
    }
}
