package com.fooddelivery.user;

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
    @DisplayName("User Service configuration must only use userdb and never other microservice databases")
    void testUserServiceDatabaseIsolation() throws Exception {
        Path configPath = Paths.get("../../config-repo/user-service.yml");
        if (!Files.exists(configPath)) {
            configPath = Paths.get("../config-repo/user-service.yml");
        }
        if (!Files.exists(configPath)) {
            configPath = Paths.get("config-repo/user-service.yml");
        }
        assertThat(Files.exists(configPath)).isTrue();

        Yaml yaml = new Yaml();
        try (InputStream in = Files.newInputStream(configPath)) {
            Map<String, Object> config = yaml.load(in);
            String rawYaml = Files.readString(configPath);

            assertThat(rawYaml).contains("userdb");
            assertThat(rawYaml).doesNotContain("fooddb");
            assertThat(rawYaml).doesNotContain("orderdb");
            assertThat(rawYaml).doesNotContain("paymentdb");
            assertThat(rawYaml).doesNotContain("notificationdb");
        }
    }
}
