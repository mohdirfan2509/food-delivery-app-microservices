package com.fooddelivery.user.security;

import com.fooddelivery.user.entity.Customer;
import com.fooddelivery.user.repository.CustomerRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final CustomerRepository customerRepository;

    public CustomUserDetailsService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalizedEmail = email != null ? email.trim().toLowerCase() : "";
        Customer customer = customerRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Customer not found with email: " + email));
        return UserPrincipal.create(customer);
    }
}
