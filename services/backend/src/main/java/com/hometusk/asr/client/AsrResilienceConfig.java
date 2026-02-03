package com.hometusk.asr.client;

import com.hometusk.asr.exception.AsrUnavailableException;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.net.SocketTimeoutException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.ResourceAccessException;

@Configuration
@EnableConfigurationProperties({AsrResilienceProperties.class, AsrProperties.class})
public class AsrResilienceConfig {

    @Bean
    @Primary
    public RetryRegistry asrRetryRegistry(
            @Qualifier("aiPlatformRetryRegistry") RetryRegistry retryRegistry, AsrResilienceProperties properties) {
        registerAsrRetry(retryRegistry, properties);
        return retryRegistry;
    }

    static void registerAsrRetry(RetryRegistry retryRegistry, AsrResilienceProperties properties) {
        AsrResilienceProperties.Retry retryProps = properties.getRetry();

        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(retryProps.getMaxAttempts())
                .intervalFunction(IntervalFunction.ofExponentialBackoff(
                        retryProps.getWaitDurationMs(), retryProps.getBackoffMultiplier()))
                .retryOnException(AsrResilienceConfig::isRetryable)
                .build();

        retryRegistry.retry("asr", retryConfig);
    }

    private static boolean isRetryable(Throwable throwable) {
        if (throwable instanceof AsrUnavailableException) {
            return true;
        }
        if (throwable instanceof SocketTimeoutException) {
            return true;
        }
        if (throwable instanceof ResourceAccessException resourceAccessException) {
            return resourceAccessException.getCause() instanceof SocketTimeoutException;
        }
        return false;
    }
}
