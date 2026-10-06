package com.fooddelivery.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayConfigFileTest {

    @Test
    @DisplayName("Verify config-repo/api-gateway.yml configuration completeness and isolation")
    void testGatewayConfigVerification() throws Exception {
        Path configPath = Paths.get("../config-repo/api-gateway.yml");
        if (!Files.exists(configPath)) {
            configPath = Paths.get("config-repo/api-gateway.yml");
        }
        assertThat(Files.exists(configPath)).isTrue();

        Yaml yaml = new Yaml();
        try (InputStream in = Files.newInputStream(configPath)) {
            Map<String, Object> config = yaml.load(in);
            String rawYaml = Files.readString(configPath);

            // Port 8080
            assertThat(rawYaml).contains("8080");

            // All 5 microservice routes
            assertThat(rawYaml).contains("user-service");
            assertThat(rawYaml).contains("food-service");
            assertThat(rawYaml).contains("order-service");
            assertThat(rawYaml).contains("payment-service");
            assertThat(rawYaml).contains("notification-service");

            // CORS origin
            assertThat(rawYaml).contains("http://localhost:3000");

            // API Gateway must NOT own or configure any database
            assertThat(rawYaml).doesNotContain("userdb");
            assertThat(rawYaml).doesNotContain("fooddb");
            assertThat(rawYaml).doesNotContain("orderdb");
            assertThat(rawYaml).doesNotContain("paymentdb");
            assertThat(rawYaml).doesNotContain("notificationdb");
        }
    }
}
