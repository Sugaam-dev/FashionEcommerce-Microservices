package com.pmrgsolution.orderservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
    "eureka.client.enabled=false"
})
@ActiveProfiles("test")
class OrderServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
