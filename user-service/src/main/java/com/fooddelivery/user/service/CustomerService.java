package com.fooddelivery.user.service;

import com.fooddelivery.user.dto.CustomerResponse;
import com.fooddelivery.user.dto.LoginRequest;
import com.fooddelivery.user.dto.LoginResponse;
import com.fooddelivery.user.dto.RegisterRequest;
import com.fooddelivery.user.dto.UpdateCustomerRequest;
import com.fooddelivery.user.security.UserPrincipal;

public interface CustomerService {

    CustomerResponse registerCustomer(RegisterRequest request);

    LoginResponse authenticateCustomer(LoginRequest request);

    CustomerResponse getCustomerById(Long id, UserPrincipal currentUser);

    CustomerResponse updateCustomer(Long id, UpdateCustomerRequest request, UserPrincipal currentUser);

    void deleteCustomer(Long id, UserPrincipal currentUser);
}
