package com.avragerghost.request_logger_aop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("request-logger")
public class LoggingProperties {

    private boolean enabled = false;
    private LoggingLevel loggingLevel = LoggingLevel.ALL;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LoggingLevel getLoggingLevel() {
        return loggingLevel;
    }

    public void setLoggingLevel(LoggingLevel loggingLevel) {
        this.loggingLevel = loggingLevel;
    }
}
