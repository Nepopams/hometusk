package com.hometusk.notifications.email.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "hometusk.email")
public class EmailNotificationProperties {

    private boolean enabled = false;
    private String sender = "log";
    private String from = "noreply@hometusk.local";
    private int fixedRateMs = 60000;
    private int batchSize = 25;
    private int maxAttempts = 3;
    private long retryDelayMs = 60000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender == null || sender.isBlank() ? "log" : sender.trim();
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from == null || from.isBlank() ? "noreply@hometusk.local" : from.trim();
    }

    public int getFixedRateMs() {
        return fixedRateMs;
    }

    public void setFixedRateMs(int fixedRateMs) {
        this.fixedRateMs = Math.max(1000, fixedRateMs);
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = Math.max(1, batchSize);
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    public long getRetryDelayMs() {
        return retryDelayMs;
    }

    public void setRetryDelayMs(long retryDelayMs) {
        this.retryDelayMs = Math.max(0, retryDelayMs);
    }

    public Duration retryDelay() {
        return Duration.ofMillis(retryDelayMs);
    }
}
