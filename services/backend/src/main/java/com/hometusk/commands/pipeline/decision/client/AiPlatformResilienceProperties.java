package com.hometusk.commands.pipeline.decision.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aiplatform.resilience")
public class AiPlatformResilienceProperties {

    private final Retry retry = new Retry();
    private final CircuitBreaker circuitBreaker = new CircuitBreaker();

    public Retry getRetry() {
        return retry;
    }

    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }

    public static class Retry {
        private int maxAttempts = 2;
        private long initialIntervalMs = 200;
        private double backoffMultiplier = 2.0;
        private double jitterFactor = 0.5;

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public long getInitialIntervalMs() {
            return initialIntervalMs;
        }

        public void setInitialIntervalMs(long initialIntervalMs) {
            this.initialIntervalMs = initialIntervalMs;
        }

        public double getBackoffMultiplier() {
            return backoffMultiplier;
        }

        public void setBackoffMultiplier(double backoffMultiplier) {
            this.backoffMultiplier = backoffMultiplier;
        }

        public double getJitterFactor() {
            return jitterFactor;
        }

        public void setJitterFactor(double jitterFactor) {
            this.jitterFactor = jitterFactor;
        }
    }

    public static class CircuitBreaker {
        private int slidingWindowSize = 10;
        private int minimumNumberOfCalls = 5;
        private float failureRateThreshold = 50.0f;
        private long waitDurationInOpenStateMs = 30000;
        private int permittedCallsInHalfOpenState = 2;

        public int getSlidingWindowSize() {
            return slidingWindowSize;
        }

        public void setSlidingWindowSize(int slidingWindowSize) {
            this.slidingWindowSize = slidingWindowSize;
        }

        public int getMinimumNumberOfCalls() {
            return minimumNumberOfCalls;
        }

        public void setMinimumNumberOfCalls(int minimumNumberOfCalls) {
            this.minimumNumberOfCalls = minimumNumberOfCalls;
        }

        public float getFailureRateThreshold() {
            return failureRateThreshold;
        }

        public void setFailureRateThreshold(float failureRateThreshold) {
            this.failureRateThreshold = failureRateThreshold;
        }

        public long getWaitDurationInOpenStateMs() {
            return waitDurationInOpenStateMs;
        }

        public void setWaitDurationInOpenStateMs(long waitDurationInOpenStateMs) {
            this.waitDurationInOpenStateMs = waitDurationInOpenStateMs;
        }

        public int getPermittedCallsInHalfOpenState() {
            return permittedCallsInHalfOpenState;
        }

        public void setPermittedCallsInHalfOpenState(int permittedCallsInHalfOpenState) {
            this.permittedCallsInHalfOpenState = permittedCallsInHalfOpenState;
        }
    }
}
