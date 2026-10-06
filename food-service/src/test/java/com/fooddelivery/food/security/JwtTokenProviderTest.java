package com.fooddelivery.food.security;

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
    @DisplayName("Should generate and validate valid ADMIN token")
    void testValidAdminToken() {
        String token = jwtTokenProvider.generateToken(1L, "admin@foodapp.com", "ADMIN");

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals("admin@foodapp.com", jwtTokenProvider.extractEmail(token));
        assertEquals("ADMIN", jwtTokenProvider.extractRole(token));
        assertEquals(1L, jwtTokenProvider.extractCustomerId(token));
    }

    @Test
    @DisplayName("Should generate and validate valid CUSTOMER token")
    void testValidCustomerToken() {
        String token = jwtTokenProvider.generateToken(2L, "customer@foodapp.com", "CUSTOMER");

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals("customer@foodapp.com", jwtTokenProvider.extractEmail(token));
        assertEquals("CUSTOMER", jwtTokenProvider.extractRole(token));
        assertEquals(2L, jwtTokenProvider.extractCustomerId(token));
    }

    @Test
    @DisplayName("Should reject invalid or tampered JWT token")
    void testInvalidToken() {
        String token = jwtTokenProvider.generateToken(1L, "admin@foodapp.com", "ADMIN");
        String tamperedToken = token + "corrupted";

        assertFalse(jwtTokenProvider.validateToken(tamperedToken));
    }

    @Test
    @DisplayName("Should reject expired JWT token")
    void testExpiredToken() {
        JwtTokenProvider expiredProvider = new JwtTokenProvider(secret, -1000L);
        String expiredToken = expiredProvider.generateToken(1L, "admin@foodapp.com", "ADMIN");

        assertFalse(jwtTokenProvider.validateToken(expiredToken));
    }
}
