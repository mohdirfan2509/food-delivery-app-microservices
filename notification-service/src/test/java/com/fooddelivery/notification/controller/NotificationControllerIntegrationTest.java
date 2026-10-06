package com.fooddelivery.notification.controller;

import com.fooddelivery.notification.entity.Notification;
import com.fooddelivery.notification.entity.NotificationStatus;
import com.fooddelivery.notification.entity.NotificationType;
import com.fooddelivery.notification.repository.NotificationRepository;
import com.fooddelivery.notification.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String customer1Token;
    private String customer2Token;
    private String adminToken;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        customer1Token = "Bearer " + jwtTokenProvider.generateToken(1L, "cust1@foodapp.com", "CUSTOMER");
        customer2Token = "Bearer " + jwtTokenProvider.generateToken(2L, "cust2@foodapp.com", "CUSTOMER");
        adminToken = "Bearer " + jwtTokenProvider.generateToken(999L, "admin@foodapp.com", "ADMIN");
    }

    @Test
    @DisplayName("GET /notifications/my - Unauthenticated returns 401")
    void testGetMyNotificationsUnauthenticated() throws Exception {
        mockMvc.perform(get("/notifications/my"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("GET /notifications/my - Returns only authenticated customer notifications")
    void testGetMyNotificationsSuccess() throws Exception {
        Notification notif1 = new Notification(101L, 1L, NotificationType.ORDER_CREATED, "Order 101 created", NotificationStatus.UNREAD);
        Notification notif2 = new Notification(102L, 2L, NotificationType.ORDER_CREATED, "Order 102 created", NotificationStatus.UNREAD);
        notificationRepository.save(notif1);
        notificationRepository.save(notif2);

        mockMvc.perform(get("/notifications/my")
                        .header(HttpHeaders.AUTHORIZATION, customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].orderId").value(101))
                .andExpect(jsonPath("$[0].customerId").value(1));
    }

    @Test
    @DisplayName("GET /notifications/{id} - Owner customer can access notification")
    void testGetNotificationByIdOwnerSuccess() throws Exception {
        Notification notif = new Notification(201L, 1L, NotificationType.ORDER_CREATED, "Order 201 created", NotificationStatus.UNREAD);
        Notification saved = notificationRepository.save(notif);

        mockMvc.perform(get("/notifications/" + saved.getNotificationId())
                        .header(HttpHeaders.AUTHORIZATION, customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(201))
                .andExpect(jsonPath("$.status").value("UNREAD"));
    }

    @Test
    @DisplayName("GET /notifications/{id} - Non-owner customer receives 403 Forbidden")
    void testGetNotificationByIdNonOwnerForbidden() throws Exception {
        Notification notif = new Notification(301L, 1L, NotificationType.ORDER_CREATED, "Order 301 created", NotificationStatus.UNREAD);
        Notification saved = notificationRepository.save(notif);

        mockMvc.perform(get("/notifications/" + saved.getNotificationId())
                        .header(HttpHeaders.AUTHORIZATION, customer2Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("GET /notifications/{id} - Admin can access any customer notification")
    void testGetNotificationByIdAdminSuccess() throws Exception {
        Notification notif = new Notification(401L, 1L, NotificationType.ORDER_CREATED, "Order 401 created", NotificationStatus.UNREAD);
        Notification saved = notificationRepository.save(notif);

        mockMvc.perform(get("/notifications/" + saved.getNotificationId())
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(401));
    }

    @Test
    @DisplayName("PATCH /notifications/{id}/read - Owner can mark notification as READ")
    void testMarkAsReadOwnerSuccess() throws Exception {
        Notification notif = new Notification(501L, 1L, NotificationType.ORDER_CREATED, "Order 501 created", NotificationStatus.UNREAD);
        Notification saved = notificationRepository.save(notif);

        mockMvc.perform(patch("/notifications/" + saved.getNotificationId() + "/read")
                        .header(HttpHeaders.AUTHORIZATION, customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READ"));

        Notification updated = notificationRepository.findById(saved.getNotificationId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(NotificationStatus.READ);
    }

    @Test
    @DisplayName("PATCH /notifications/{id}/read - Non-owner receives 403 Forbidden")
    void testMarkAsReadNonOwnerForbidden() throws Exception {
        Notification notif = new Notification(601L, 1L, NotificationType.ORDER_CREATED, "Order 601 created", NotificationStatus.UNREAD);
        Notification saved = notificationRepository.save(notif);

        mockMvc.perform(patch("/notifications/" + saved.getNotificationId() + "/read")
                        .header(HttpHeaders.AUTHORIZATION, customer2Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("GET /notifications/{id} - Not found returns 404")
    void testGetNotificationNotFound() throws Exception {
        mockMvc.perform(get("/notifications/99999")
                        .header(HttpHeaders.AUTHORIZATION, customer1Token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }
}
