package com.fooddelivery.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GatewayRoutesTest {

    @Autowired
    private RouteLocator routeLocator;

    @Test
    @DisplayName("Verify all microservice routes are registered in RouteLocator")
    void testRoutesRegistration() {
        List<Route> routes = routeLocator.getRoutes().collectList().block();

        assertThat(routes).isNotNull();

        Map<String, String> routeMap = routes.stream()
                .collect(Collectors.toMap(Route::getId, r -> r.getUri().toString()));

        assertThat(routeMap).containsKey("user-service");
        assertThat(routeMap.get("user-service")).isEqualTo("http://localhost:8081");

        assertThat(routeMap).containsKey("food-service");
        assertThat(routeMap.get("food-service")).isEqualTo("http://localhost:8082");

        assertThat(routeMap).containsKey("order-service");
        assertThat(routeMap.get("order-service")).isEqualTo("http://localhost:8083");

        assertThat(routeMap).containsKey("payment-service");
        assertThat(routeMap.get("payment-service")).isEqualTo("http://localhost:8084");

        assertThat(routeMap).containsKey("notification-service");
        assertThat(routeMap.get("notification-service")).isEqualTo("http://localhost:8085");
    }
}
