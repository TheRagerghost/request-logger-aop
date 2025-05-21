package com.avragerghost.request_logger_aop.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import com.avragerghost.request_logger_aop.aspects.LoggingAspect;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableConfigurationProperties(LoggingProperties.class)
@EnableAspectJAutoProxy
public class LoggingConfig {

    private final LoggingProperties properties;

    public LoggingConfig(LoggingProperties properties) {
        this.properties = properties;
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public LoggingAspect loggingAspect() {
        return new LoggingAspect(properties, objectMapper());
    }

}
