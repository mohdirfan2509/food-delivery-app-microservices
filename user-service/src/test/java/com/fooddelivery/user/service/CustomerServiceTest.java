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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    private CustomerServiceImpl customerService;

    @BeforeEach
    void setUp() {
        customerService = new CustomerServiceImpl(
                customerRepository,
                passwordEncoder,
                authenticationManager,
                tokenProvider
        );
    }

    @Test
    @DisplayName("Registration should hash password and assign Role.CUSTOMER")
    void testRegisterCustomerSuccess() {
        RegisterRequest request = new RegisterRequest("Alice", "alice@example.com", "Password@123", "9876543210");

        when(customerRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("hashed-bcrypt-pw");

        Customer savedCustomer = new Customer("Alice", "alice@example.com", "hashed-bcrypt-pw", "9876543210", Role.CUSTOMER);
        savedCustomer.setCustomerId(1L);
        when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);

        CustomerResponse response = customerService.registerCustomer(request);

        assertThat(response).isNotNull();
        assertThat(response.getCustomerId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
        assertThat(response.getRole()).isEqualTo("CUSTOMER");

        verify(passwordEncoder).encode("Password@123");
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("Registration with duplicate email should throw DuplicateEmailException")
    void testRegisterDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("Alice", "alice@example.com", "Password@123", "9876543210");
        when(customerRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> customerService.registerCustomer(request))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("Email is already registered");

        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Login should return LoginResponse with JWT token")
    void testAuthenticateCustomerSuccess() {
        LoginRequest request = new LoginRequest("alice@example.com", "Password@123");
        Customer customer = new Customer("Alice", "alice@example.com", "hashed-bcrypt-pw", "9876543210", Role.CUSTOMER);
        customer.setCustomerId(1L);

        when(customerRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(customer));
        when(tokenProvider.generateToken(1L, "alice@example.com", Role.CUSTOMER)).thenReturn("mocked.jwt.token");

        LoginResponse response = customerService.authenticateCustomer(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("mocked.jwt.token");
        assertThat(response.getCustomerId()).isEqualTo(1L);
        assertThat(response.getRole()).isEqualTo("CUSTOMER");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Login with invalid credentials should rethrow BadCredentialsException")
    void testAuthenticateInvalidCredentials() {
        LoginRequest request = new LoginRequest("alice@example.com", "WrongPassword");
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThatThrownBy(() -> customerService.authenticateCustomer(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("Customer accessing own profile should succeed")
    void testGetOwnProfileSuccess() {
        UserPrincipal principal = new UserPrincipal(1L, "Alice", "alice@example.com", "pw", Role.CUSTOMER, List.of());
        Customer customer = new Customer("Alice", "alice@example.com", "pw", "9876543210", Role.CUSTOMER);
        customer.setCustomerId(1L);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        CustomerResponse response = customerService.getCustomerById(1L, principal);
        assertThat(response.getCustomerId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Customer accessing another customer's profile should throw ForbiddenException")
    void testGetOtherProfileForbidden() {
        UserPrincipal principal = new UserPrincipal(1L, "Alice", "alice@example.com", "pw", Role.CUSTOMER, List.of());

        assertThatThrownBy(() -> customerService.getCustomerById(2L, principal))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("not authorized");
    }

    @Test
    @DisplayName("Admin accessing any customer's profile should succeed")
    void testAdminAccessOtherCustomerSuccess() {
        UserPrincipal adminPrincipal = new UserPrincipal(99L, "Admin", "admin@foodapp.com", "pw", Role.ADMIN, List.of());
        Customer customer = new Customer("Alice", "alice@example.com", "pw", "9876543210", Role.CUSTOMER);
        customer.setCustomerId(1L);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        CustomerResponse response = customerService.getCustomerById(1L, adminPrincipal);
        assertThat(response.getCustomerId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Customer cannot delete their own profile (only Admin permitted)")
    void testCustomerCannotDeleteOwnProfile() {
        UserPrincipal principal = new UserPrincipal(1L, "Alice", "alice@example.com", "pw", Role.CUSTOMER, List.of());

        assertThatThrownBy(() -> customerService.deleteCustomer(1L, principal))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Only administrators are permitted");

        verify(customerRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Customer cannot delete another customer's profile")
    void testCustomerCannotDeleteOther() {
        UserPrincipal principal = new UserPrincipal(1L, "Alice", "alice@example.com", "pw", Role.CUSTOMER, List.of());

        assertThatThrownBy(() -> customerService.deleteCustomer(2L, principal))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Only administrators are permitted");

        verify(customerRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Admin can delete any customer")
    void testAdminCanDeleteCustomer() {
        UserPrincipal adminPrincipal = new UserPrincipal(99L, "Admin", "admin@foodapp.com", "pw", Role.ADMIN, List.of());
        when(customerRepository.existsById(1L)).thenReturn(true);

        customerService.deleteCustomer(1L, adminPrincipal);
        verify(customerRepository).deleteById(1L);
    }
}
