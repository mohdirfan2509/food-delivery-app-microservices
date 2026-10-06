package com.fooddelivery.order;

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
    @DisplayName("Order Service configuration must only use orderdb and never other microservice databases")
    void testOrderServiceDatabaseIsolation() throws Exception {
        Path configPath = Paths.get("../config-repo/order-service.yml");
        if (!Files.exists(configPath)) {
            configPath = Paths.get("config-repo/order-service.yml");
        }
        assertThat(Files.exists(configPath)).isTrue();

        Yaml yaml = new Yaml();
        try (InputStream in = Files.newInputStream(configPath)) {
            Map<String, Object> config = yaml.load(in);
            String rawYaml = Files.readString(configPath);

            assertThat(rawYaml).contains("orderdb");
            assertThat(rawYaml).doesNotContain("userdb");
            assertThat(rawYaml).doesNotContain("fooddb");
            assertThat(rawYaml).doesNotContain("paymentdb");
        }
    }
}
