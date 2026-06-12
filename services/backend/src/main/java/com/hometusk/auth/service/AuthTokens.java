package com.hometusk.auth.service;

import java.time.Instant;

public record AuthTokens(String accessToken, String refreshToken, long expiresInSeconds, long refreshExpiresInSeconds) {

    public Instant accessExpiresAt() {
        return Instant.now().plusSeconds(Math.max(0, expiresInSeconds));
    }
}
