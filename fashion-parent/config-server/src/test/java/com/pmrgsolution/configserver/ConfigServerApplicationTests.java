package com.pmrgsolution.configserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
    "eureka.client.enabled=false"
})
@ActiveProfiles("native")
class ConfigServerApplicationTests {

    @Test
    void contextLoads() {
    }

}
