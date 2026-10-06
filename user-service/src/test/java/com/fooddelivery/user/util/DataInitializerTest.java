package com.fooddelivery.user.util;

import com.fooddelivery.user.entity.Customer;
import com.fooddelivery.user.entity.Role;
import com.fooddelivery.user.repository.CustomerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class DataInitializerTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DataInitializer dataInitializer;

    @Test
    @DisplayName("Admin seed account should exist with Role.ADMIN and BCrypt password")
    void testAdminSeeding() {
        Customer admin = customerRepository.findByEmail("admin@foodapp.com").orElse(null);
        assertThat(admin).isNotNull();
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(passwordEncoder.matches("Admin@1234", admin.getPassword())).isTrue();
    }

    @Test
    @DisplayName("Running DataInitializer again should not duplicate administrator")
    void testAdminSeedingIdempotency() {
        long initialCount = customerRepository.count();

        // Run again
        dataInitializer.run();

        long afterCount = customerRepository.count();
        assertThat(afterCount).isEqualTo(initialCount);
    }
}
