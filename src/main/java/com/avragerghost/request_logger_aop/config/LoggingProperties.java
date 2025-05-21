package com.avragerghost.request_logger_aop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("request-logger")
public class LoggingProperties {

    private boolean enabled = false;

    private String loggingLevel = "all";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getLoggingLevel() {
        return loggingLevel;
    }

    public void setLoggingLevel(String loggingLevel) {
        this.loggingLevel = loggingLevel;
    }

}
