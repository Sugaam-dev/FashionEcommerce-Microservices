package com.pmrgsolution.authservice;

import com.pmrgsolution.authservice.repository.AuditLogRepository;
import com.pmrgsolution.authservice.repository.EmailOtpRepository;
import com.pmrgsolution.authservice.repository.OutboxEventRepository;
import com.pmrgsolution.authservice.repository.UserAddressRepository;
import com.pmrgsolution.authservice.repository.UserRepository;
import com.pmrgsolution.authservice.repository.UserSessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "eureka.client.enabled=false"
})
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        FlywayAutoConfiguration.class,
        RedisAutoConfiguration.class,
        KafkaAutoConfiguration.class
})
class AuthServiceApplicationTests {

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserAddressRepository userAddressRepository;

    @MockBean
    private EmailOtpRepository emailOtpRepository;

    @MockBean
    private UserSessionRepository userSessionRepository;

    @MockBean
    private OutboxEventRepository outboxEventRepository;

    @MockBean
    private AuditLogRepository auditLogRepository;

    @MockBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void contextLoads() {
        assertTrue(true);
    }
}
