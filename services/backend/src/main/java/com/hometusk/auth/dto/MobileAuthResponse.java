package com.hometusk.auth.dto;

import com.hometusk.auth.service.AuthTokens;

public record MobileAuthResponse(
        String accessToken,
        String refreshToken,
        long expiresInSeconds,
        long refreshExpiresInSeconds,
        String tokenType) {

    public static MobileAuthResponse from(AuthTokens tokens) {
        return new MobileAuthResponse(
                tokens.accessToken(),
                tokens.refreshToken(),
                tokens.expiresInSeconds(),
                tokens.refreshExpiresInSeconds(),
                "Bearer");
    }
}
