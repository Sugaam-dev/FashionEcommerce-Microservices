package com.pmrgsolution.paymentservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "eureka.client.enabled=false"
})
class PaymentServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
