package com.pmrgsolution.inventoryservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
    "eureka.client.enabled=false"
})
@ActiveProfiles("test")
class InventoryServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
