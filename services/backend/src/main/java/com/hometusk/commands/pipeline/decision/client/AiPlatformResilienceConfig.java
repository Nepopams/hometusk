package com.hometusk.commands.pipeline.decision.client;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.IntervalFunction;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClientException;

@Configuration
@EnableConfigurationProperties(AiPlatformResilienceProperties.class)
public class AiPlatformResilienceConfig {

    @Bean
    public RetryRegistry aiPlatformRetryRegistry(AiPlatformResilienceProperties properties) {
        AiPlatformResilienceProperties.Retry retryProps = properties.getRetry();

        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(retryProps.getMaxAttempts())
                .intervalFunction(IntervalFunction.ofExponentialRandomBackoff(
                        retryProps.getInitialIntervalMs(),
                        retryProps.getBackoffMultiplier(),
                        retryProps.getJitterFactor()))
                .retryExceptions(AiPlatformException.class, RestClientException.class)
                .build();

        RetryRegistry registry = RetryRegistry.ofDefaults();
        registry.retry("aiPlatform", retryConfig);
        return registry;
    }

    @Bean
    public CircuitBreakerRegistry aiPlatformCircuitBreakerRegistry(AiPlatformResilienceProperties properties) {
        AiPlatformResilienceProperties.CircuitBreaker circuitProps = properties.getCircuitBreaker();

        CircuitBreakerConfig circuitConfig = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(circuitProps.getSlidingWindowSize())
                .minimumNumberOfCalls(circuitProps.getMinimumNumberOfCalls())
                .failureRateThreshold(circuitProps.getFailureRateThreshold())
                .waitDurationInOpenState(Duration.ofMillis(circuitProps.getWaitDurationInOpenStateMs()))
                .permittedNumberOfCallsInHalfOpenState(circuitProps.getPermittedCallsInHalfOpenState())
                .recordExceptions(AiPlatformException.class, RestClientException.class)
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        registry.circuitBreaker("aiPlatform", circuitConfig);
        return registry;
    }
}
