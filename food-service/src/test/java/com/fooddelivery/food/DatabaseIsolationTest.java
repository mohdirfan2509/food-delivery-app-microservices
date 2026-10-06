package com.fooddelivery.food;

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
    @DisplayName("Food Service configuration must only use fooddb and never other microservice databases")
    void testFoodServiceDatabaseIsolation() throws Exception {
        Path configPath = Paths.get("../../config-repo/food-service.yml");
        if (!Files.exists(configPath)) {
            configPath = Paths.get("../config-repo/food-service.yml");
        }
        if (!Files.exists(configPath)) {
            configPath = Paths.get("config-repo/food-service.yml");
        }
        assertThat(Files.exists(configPath)).isTrue();

        Yaml yaml = new Yaml();
        try (InputStream in = Files.newInputStream(configPath)) {
            Map<String, Object> config = yaml.load(in);
            String rawYaml = Files.readString(configPath);

            assertThat(rawYaml).contains("fooddb");
            assertThat(rawYaml).doesNotContain("userdb");
            assertThat(rawYaml).doesNotContain("orderdb");
            assertThat(rawYaml).doesNotContain("paymentdb");
            assertThat(rawYaml).doesNotContain("notificationdb");
        }
    }
}
