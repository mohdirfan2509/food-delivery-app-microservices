package com.fooddelivery.payment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseIsolationTest {

    @Test
    @DisplayName("Payment Service configuration must only use paymentdb and never other microservice databases")
    void testPaymentServiceDatabaseIsolation() throws Exception {
        Path configPath = Paths.get("../config-repo/payment-service.yml");
        if (!Files.exists(configPath)) {
            configPath = Paths.get("config-repo/payment-service.yml");
        }
        assertThat(Files.exists(configPath)).isTrue();

        Yaml yaml = new Yaml();
        try (InputStream in = Files.newInputStream(configPath)) {
            Map<String, Object> config = yaml.load(in);
            String rawYaml = Files.readString(configPath);

            assertThat(rawYaml).contains("paymentdb");
            assertThat(rawYaml).doesNotContain("userdb");
            assertThat(rawYaml).doesNotContain("fooddb");
            assertThat(rawYaml).doesNotContain("orderdb");
        }
    }
}
