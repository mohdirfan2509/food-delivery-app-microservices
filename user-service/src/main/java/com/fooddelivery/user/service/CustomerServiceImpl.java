package com.fooddelivery.user.service;

import com.fooddelivery.user.dto.CustomerResponse;
import com.fooddelivery.user.dto.LoginRequest;
import com.fooddelivery.user.dto.LoginResponse;
import com.fooddelivery.user.dto.RegisterRequest;
import com.fooddelivery.user.dto.UpdateCustomerRequest;
import com.fooddelivery.user.entity.Customer;
import com.fooddelivery.user.entity.Role;
import com.fooddelivery.user.exception.DuplicateEmailException;
import com.fooddelivery.user.exception.ForbiddenException;
import com.fooddelivery.user.exception.ResourceNotFoundException;
import com.fooddelivery.user.repository.CustomerRepository;
import com.fooddelivery.user.security.JwtTokenProvider;
import com.fooddelivery.user.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerServiceImpl implements CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerServiceImpl.class);

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    public CustomerServiceImpl(
            CustomerRepository customerRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtTokenProvider tokenProvider) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
    }

    @Override
    @Transactional
    public CustomerResponse registerCustomer(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (customerRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException("Email is already registered: " + normalizedEmail);
        }

        // Enforce that public registration always assigns Role.CUSTOMER
        Customer customer = new Customer(
                request.getName().trim(),
                normalizedEmail,
                passwordEncoder.encode(request.getPassword()),
                request.getPhone() != null ? request.getPhone().trim() : null,
                Role.CUSTOMER
        );

        Customer saved = customerRepository.save(customer);
        log.info("Registered new customer with ID: {} and email: {}", saved.getCustomerId(), saved.getEmail());

        return toCustomerResponse(saved);
    }

    @Override
    public LoginResponse authenticateCustomer(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
        );

        Customer customer = customerRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with email: " + normalizedEmail));

        String token = tokenProvider.generateToken(customer.getCustomerId(), customer.getEmail(), customer.getRole());
        log.info("Successfully authenticated customer ID: {} with role: {}", customer.getCustomerId(), customer.getRole());

        return new LoginResponse(
                token,
                customer.getCustomerId(),
                customer.getName(),
                customer.getEmail(),
                customer.getRole().name()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long id, UserPrincipal currentUser) {
        validateAccess(id, currentUser, "view");

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));

        return toCustomerResponse(customer);
    }

    @Override
    @Transactional
    public CustomerResponse updateCustomer(Long id, UpdateCustomerRequest request, UserPrincipal currentUser) {
        validateAccess(id, currentUser, "update");

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));

        customer.setName(request.getName().trim());
        if (request.getPhone() != null) {
            customer.setPhone(request.getPhone().trim());
        }

        Customer updated = customerRepository.save(customer);
        log.info("Updated customer profile ID: {}", updated.getCustomerId());

        return toCustomerResponse(updated);
    }

    @Override
    @Transactional
    public void deleteCustomer(Long id, UserPrincipal currentUser) {
        if (!currentUser.isAdmin()) {
            throw new ForbiddenException("Only administrators are permitted to delete customer accounts");
        }

        if (!customerRepository.existsById(id)) {
            throw new ResourceNotFoundException("Customer not found with id: " + id);
        }

        customerRepository.deleteById(id);
        log.info("Deleted customer with ID: {}", id);
    }

    private void validateAccess(Long targetCustomerId, UserPrincipal currentUser, String operation) {
        if (currentUser.isAdmin()) {
            return; // Administrator has global access
        }

        if (!currentUser.getCustomerId().equals(targetCustomerId)) {
            throw new ForbiddenException("You are not authorized to " + operation + " another customer's profile");
        }
    }

    private CustomerResponse toCustomerResponse(Customer customer) {
        return new CustomerResponse(
                customer.getCustomerId(),
                customer.getName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getRole().name()
        );
    }
}
