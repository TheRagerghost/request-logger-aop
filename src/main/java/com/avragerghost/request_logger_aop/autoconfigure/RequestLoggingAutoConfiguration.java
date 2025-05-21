package com.avragerghost.request_logger_aop.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

import com.avragerghost.request_logger_aop.config.LoggingConfig;

@AutoConfiguration
@ConditionalOnClass(com.avragerghost.request_logger_aop.aspects.LoggingAspect.class)
@ConditionalOnProperty(prefix = "request-logging", name = "enabled", havingValue = "true", matchIfMissing = false)
@Import(LoggingConfig.class)
public class RequestLoggingAutoConfiguration {
}