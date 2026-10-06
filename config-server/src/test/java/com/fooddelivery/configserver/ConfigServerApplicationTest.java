package com.fooddelivery.configserver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.cloud.config.server.git.uri=file:../config-repo",
        "spring.cloud.config.server.git.clone-on-start=false"
})
class ConfigServerApplicationTest {

    @Test
    @DisplayName("Config Server context should load successfully")
    void contextLoads() {
    }
}
