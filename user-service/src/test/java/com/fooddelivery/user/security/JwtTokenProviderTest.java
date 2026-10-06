package com.fooddelivery.user.security;

import com.fooddelivery.user.entity.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private static final String TEST_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRATION_MS = 3600000; // 1 hour

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(TEST_SECRET, EXPIRATION_MS);
    }

    @Test
    @DisplayName("Should generate valid JWT token with correct claims")
    void testGenerateToken() {
        String token = tokenProvider.generateToken(101L, "alice@foodapp.com", Role.CUSTOMER);

        assertThat(token).isNotBlank();
        assertThat(tokenProvider.validateToken(token)).isTrue();
        assertThat(tokenProvider.extractEmail(token)).isEqualTo("alice@foodapp.com");
        assertThat(tokenProvider.extractCustomerId(token)).isEqualTo(101L);
        assertThat(tokenProvider.extractRole(token)).isEqualTo("CUSTOMER");
    }

    @Test
    @DisplayName("Should reject expired JWT token")
    void testExpiredToken() {
        JwtTokenProvider expiredProvider = new JwtTokenProvider(TEST_SECRET, -1000); // already expired
        String token = expiredProvider.generateToken(102L, "bob@foodapp.com", Role.CUSTOMER);

        assertThat(tokenProvider.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("Should reject malformed or tampered JWT token")
    void testMalformedToken() {
        assertThat(tokenProvider.validateToken("invalid.token.signature")).isFalse();
        assertThat(tokenProvider.validateToken("")).isFalse();
        assertThat(tokenProvider.validateToken(null)).isFalse();
    }

    @Test
    @DisplayName("Should reject token signed with different secret key")
    void testTokenWithDifferentSecret() {
        String otherSecret = "999E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
        JwtTokenProvider otherProvider = new JwtTokenProvider(otherSecret, EXPIRATION_MS);
        String token = otherProvider.generateToken(103L, "charlie@foodapp.com", Role.ADMIN);

        assertThat(tokenProvider.validateToken(token)).isFalse();
    }
}
