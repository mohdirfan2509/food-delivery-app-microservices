package com.fooddelivery.user.util;

import com.fooddelivery.user.entity.Customer;
import com.fooddelivery.user.entity.Role;
import com.fooddelivery.user.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@foodapp.com}")
    private String adminEmail;

    @Value("${app.admin.password:Admin@1234}")
    private String adminPassword;

    @Value("${app.admin.name:System Administrator}")
    private String adminName;

    @Value("${app.admin.phone:9876543210}")
    private String adminPhone;

    public DataInitializer(CustomerRepository customerRepository, PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedAdminUser();
        seedCustomerUser();
    }

    private void seedAdminUser() {
        String normalizedAdminEmail = adminEmail.trim().toLowerCase();
        if (!customerRepository.existsByEmail(normalizedAdminEmail)) {
            Customer admin = new Customer(
                    adminName.trim(),
                    normalizedAdminEmail,
                    passwordEncoder.encode(adminPassword),
                    adminPhone.trim(),
                    Role.ADMIN
            );
            customerRepository.save(admin);
            log.info("Initialized default seed administrator account: {} (Role: ADMIN)", normalizedAdminEmail);
        } else {
            log.info("Default seed administrator already exists: {}", normalizedAdminEmail);
        }
    }

    private void seedCustomerUser() {
        String defaultCustomerEmail = "customer@foodapp.com";
        if (!customerRepository.existsByEmail(defaultCustomerEmail)) {
            Customer customer = new Customer(
                    "Demo Customer",
                    defaultCustomerEmail,
                    passwordEncoder.encode("Customer@1234"),
                    "9876543211",
                    Role.CUSTOMER
            );
            customerRepository.save(customer);
            log.info("Initialized default seed customer account: {} (Role: CUSTOMER)", defaultCustomerEmail);
        }
    }
}
