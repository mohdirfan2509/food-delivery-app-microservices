package com.fooddelivery.notification;

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
    @DisplayName("Notification Service configuration must only use notificationdb and never other microservice databases")
    void testNotificationServiceDatabaseIsolation() throws Exception {
        Path configPath = Paths.get("../config-repo/notification-service.yml");
        if (!Files.exists(configPath)) {
            configPath = Paths.get("config-repo/notification-service.yml");
        }
        assertThat(Files.exists(configPath)).isTrue();

        Yaml yaml = new Yaml();
        try (InputStream in = Files.newInputStream(configPath)) {
            Map<String, Object> config = yaml.load(in);
            String rawYaml = Files.readString(configPath);

            assertThat(rawYaml).contains("notificationdb");
            assertThat(rawYaml).doesNotContain("userdb");
            assertThat(rawYaml).doesNotContain("fooddb");
            assertThat(rawYaml).doesNotContain("orderdb");
            assertThat(rawYaml).doesNotContain("paymentdb");
        }
    }
}
