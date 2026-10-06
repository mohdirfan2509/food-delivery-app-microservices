package com.fooddelivery.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.user.dto.LoginRequest;
import com.fooddelivery.user.dto.RegisterRequest;
import com.fooddelivery.user.dto.UpdateCustomerRequest;
import com.fooddelivery.user.entity.Customer;
import com.fooddelivery.user.entity.Role;
import com.fooddelivery.user.repository.CustomerRepository;
import com.fooddelivery.user.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private Customer testCustomer1;
    private Customer testCustomer2;
    private Customer adminUser;
    private String customer1Token;
    private String customer2Token;
    private String adminToken;

    @BeforeEach
    void setUp() {
        customerRepository.deleteAll();

        // Customer 1
        testCustomer1 = new Customer("Alice Smith", "alice@example.com", passwordEncoder.encode("Password@123"), "1111111111", Role.CUSTOMER);
        testCustomer1 = customerRepository.save(testCustomer1);
        customer1Token = tokenProvider.generateToken(testCustomer1.getCustomerId(), testCustomer1.getEmail(), testCustomer1.getRole());

        // Customer 2
        testCustomer2 = new Customer("Bob Jones", "bob@example.com", passwordEncoder.encode("Password@123"), "2222222222", Role.CUSTOMER);
        testCustomer2 = customerRepository.save(testCustomer2);
        customer2Token = tokenProvider.generateToken(testCustomer2.getCustomerId(), testCustomer2.getEmail(), testCustomer2.getRole());

        // Admin User
        adminUser = new Customer("Admin User", "admin@foodapp.com", passwordEncoder.encode("Admin@1234"), "9999999999", Role.ADMIN);
        adminUser = customerRepository.save(adminUser);
        adminToken = tokenProvider.generateToken(adminUser.getCustomerId(), adminUser.getEmail(), adminUser.getRole());
    }

    @Test
    @DisplayName("POST /users/register - Should register customer and always set Role.CUSTOMER")
    void testRegisterSuccess() throws Exception {
        RegisterRequest request = new RegisterRequest("Charlie", "charlie@example.com", "Password@123", "3333333333");

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("charlie@example.com"))
                .andExpect(jsonPath("$.name").value("Charlie"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.password").doesNotExist());

        Customer saved = customerRepository.findByEmail("charlie@example.com").orElseThrow();
        assertThat(saved.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(passwordEncoder.matches("Password@123", saved.getPassword())).isTrue();
    }

    @Test
    @DisplayName("POST /users/register - Should reject duplicate email with 409 Conflict")
    void testRegisterDuplicateEmail() throws Exception {
        RegisterRequest request = new RegisterRequest("Duplicate Alice", "alice@example.com", "Password@123", "4444444444");

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    @DisplayName("POST /users/register - Should reject invalid email with 400 Bad Request")
    void testRegisterInvalidEmail() throws Exception {
        RegisterRequest request = new RegisterRequest("Invalid", "not-an-email", "Password@123", "4444444444");

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /users/register - Should reject blank name with 400 Bad Request")
    void testRegisterBlankName() throws Exception {
        RegisterRequest request = new RegisterRequest("", "valid@example.com", "Password@123", "4444444444");

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /users/register - Should reject short password with 400 Bad Request")
    void testRegisterShortPassword() throws Exception {
        RegisterRequest request = new RegisterRequest("Valid Name", "valid@example.com", "123", "4444444444");

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /users/login - Should authenticate customer and return JWT")
    void testLoginSuccess() throws Exception {
        LoginRequest request = new LoginRequest("alice@example.com", "Password@123");

        mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.customerId").value(testCustomer1.getCustomerId()));
    }

    @Test
    @DisplayName("POST /users/login - Should reject wrong password with 401 Unauthorized")
    void testLoginWrongPassword() throws Exception {
        LoginRequest request = new LoginRequest("alice@example.com", "WrongPassword");

        mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /users/{id} - Should reject unauthenticated request with 401 Unauthorized")
    void testGetProfileUnauthenticated() throws Exception {
        mockMvc.perform(get("/users/" + testCustomer1.getCustomerId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /users/{id} - Customer can access their own profile (200 OK)")
    void testCustomerGetOwnProfile() throws Exception {
        mockMvc.perform(get("/users/" + testCustomer1.getCustomerId())
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(testCustomer1.getCustomerId()))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("GET /users/{id} - Customer accessing another customer profile returns 403 Forbidden")
    void testCustomerGetOtherProfileForbidden() throws Exception {
        mockMvc.perform(get("/users/" + testCustomer2.getCustomerId())
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("GET /users/{id} - Admin can access another customer's profile (200 OK)")
    void testAdminGetOtherProfileAllowed() throws Exception {
        mockMvc.perform(get("/users/" + testCustomer1.getCustomerId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(testCustomer1.getCustomerId()))
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    @DisplayName("PUT /users/{id} - Customer can update own profile (200 OK)")
    void testCustomerUpdateOwnProfile() throws Exception {
        UpdateCustomerRequest request = new UpdateCustomerRequest("Alice Updated", "9998887776");

        mockMvc.perform(put("/users/" + testCustomer1.getCustomerId())
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice Updated"))
                .andExpect(jsonPath("$.phone").value("9998887776"));
    }

    @Test
    @DisplayName("PUT /users/{id} - Customer updating another profile returns 403 Forbidden")
    void testCustomerUpdateOtherProfileForbidden() throws Exception {
        UpdateCustomerRequest request = new UpdateCustomerRequest("Hacked Name", "0000000000");

        mockMvc.perform(put("/users/" + testCustomer2.getCustomerId())
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /users/{id} - Customer attempting to delete their own profile returns 403 Forbidden")
    void testCustomerDeleteOwnProfileForbidden() throws Exception {
        mockMvc.perform(delete("/users/" + testCustomer1.getCustomerId())
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isForbidden());

        assertThat(customerRepository.existsById(testCustomer1.getCustomerId())).isTrue();
    }

    @Test
    @DisplayName("DELETE /users/{id} - Customer deleting another profile returns 403 Forbidden")
    void testCustomerDeleteOtherProfileForbidden() throws Exception {
        mockMvc.perform(delete("/users/" + testCustomer2.getCustomerId())
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isForbidden());

        assertThat(customerRepository.existsById(testCustomer2.getCustomerId())).isTrue();
    }

    @Test
    @DisplayName("DELETE /users/{id} - Admin can delete any customer profile (204 No Content)")
    void testAdminDeleteProfileAllowed() throws Exception {
        mockMvc.perform(delete("/users/" + testCustomer2.getCustomerId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        assertThat(customerRepository.existsById(testCustomer2.getCustomerId())).isFalse();
    }
}
