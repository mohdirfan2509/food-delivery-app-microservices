package com.fooddelivery.configserver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "spring.profiles.active=native",
        "spring.cloud.config.server.native.search-locations=file:../config-repo,file:./config-repo"
})
class ConfigServerApplicationTest {

    @Test
    @DisplayName("Config Server context should load successfully")
    void contextLoads() {
    }
}
