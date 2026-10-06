package com.fooddelivery.user.controller;

import com.fooddelivery.user.dto.CustomerResponse;
import com.fooddelivery.user.dto.LoginRequest;
import com.fooddelivery.user.dto.LoginResponse;
import com.fooddelivery.user.dto.RegisterRequest;
import com.fooddelivery.user.dto.UpdateCustomerRequest;
import com.fooddelivery.user.security.UserPrincipal;
import com.fooddelivery.user.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final CustomerService customerService;

    public UserController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping("/register")
    public ResponseEntity<CustomerResponse> register(@Valid @RequestBody RegisterRequest request) {
        CustomerResponse response = customerService.registerCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = customerService.authenticateCustomer(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomerById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        CustomerResponse response = customerService.getCustomerById(id, currentUser);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCustomerRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        CustomerResponse response = customerService.updateCustomer(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        customerService.deleteCustomer(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
