package com.hometusk.asr.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "asr.resilience")
public class AsrResilienceProperties {

    private final Retry retry = new Retry();

    public Retry getRetry() {
        return retry;
    }

    public static class Retry {
        private int maxAttempts = 2;
        private long waitDurationMs = 500;
        private double backoffMultiplier = 2.0;

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public long getWaitDurationMs() {
            return waitDurationMs;
        }

        public void setWaitDurationMs(long waitDurationMs) {
            this.waitDurationMs = waitDurationMs;
        }

        public double getBackoffMultiplier() {
            return backoffMultiplier;
        }

        public void setBackoffMultiplier(double backoffMultiplier) {
            this.backoffMultiplier = backoffMultiplier;
        }
    }
}
