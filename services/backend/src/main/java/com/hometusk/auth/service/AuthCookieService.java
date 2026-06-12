package com.hometusk.auth.service;

import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class AuthCookieService {

    public static final String ACCESS_COOKIE_NAME = "hometusk_token";
    public static final String REFRESH_COOKIE_NAME = "hometusk_refresh_token";

    private static final String ACCESS_COOKIE_PATH = "/";
    private static final String REFRESH_COOKIE_PATH = "/api/v1/auth";

    private final boolean secureCookie;
    private final String sameSite;

    public AuthCookieService(
            @Value("${hometusk.cookie.secure:false}") boolean secureCookie,
            @Value("${hometusk.cookie.same-site:Lax}") String sameSite) {
        this.secureCookie = secureCookie;
        this.sameSite = sameSite;
    }

    public ResponseCookie accessCookie(String token, Instant expiresAt) {
        long maxAge = expiresAt != null
                ? Math.max(0, Duration.between(Instant.now(), expiresAt).getSeconds())
                : 0;
        return cookie(ACCESS_COOKIE_NAME, token, ACCESS_COOKIE_PATH, Duration.ofSeconds(maxAge));
    }

    public ResponseCookie accessCookie(AuthTokens tokens) {
        return cookie(
                ACCESS_COOKIE_NAME,
                tokens.accessToken(),
                ACCESS_COOKIE_PATH,
                Duration.ofSeconds(Math.max(0, tokens.expiresInSeconds())));
    }

    public ResponseCookie refreshCookie(AuthTokens tokens) {
        return cookie(
                REFRESH_COOKIE_NAME,
                tokens.refreshToken(),
                REFRESH_COOKIE_PATH,
                Duration.ofSeconds(Math.max(0, tokens.refreshExpiresInSeconds())));
    }

    public ResponseCookie clearAccessCookie() {
        return cookie(ACCESS_COOKIE_NAME, "", ACCESS_COOKIE_PATH, Duration.ZERO);
    }

    public ResponseCookie clearRefreshCookie() {
        return cookie(REFRESH_COOKIE_NAME, "", REFRESH_COOKIE_PATH, Duration.ZERO);
    }

    private ResponseCookie cookie(String name, String value, String path, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(sameSite)
                .path(path)
                .maxAge(maxAge)
                .build();
    }
}
