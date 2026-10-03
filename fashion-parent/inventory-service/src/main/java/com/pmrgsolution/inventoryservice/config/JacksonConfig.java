package com.pmrgsolution.inventoryservice.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> builder
                .featuresToDisable(
                        SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
                )
                .featuresToEnable(
                        DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL
                )
                .modules(new JavaTimeModule())
                .serializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Bean
    public MappingJackson2HttpMessageConverter jsonConverter(ObjectMapper mapper) {
        mapper.getFactory().setStreamReadConstraints(
                StreamReadConstraints.builder().maxStringLength(2_000_000).build()
        );
        return new MappingJackson2HttpMessageConverter(mapper);
    }
}
