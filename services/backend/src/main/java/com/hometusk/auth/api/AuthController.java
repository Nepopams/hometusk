package com.hometusk.auth.api;

import com.hometusk.auth.dto.LoginRequest;
import com.hometusk.auth.dto.MobileAuthResponse;
import com.hometusk.auth.dto.MobileLogoutRequest;
import com.hometusk.auth.dto.MobileRefreshRequest;
import com.hometusk.auth.dto.RegisterRequest;
import com.hometusk.auth.keycloak.KeycloakAuthService;
import com.hometusk.auth.service.AuthCookieService;
import com.hometusk.auth.service.AuthTokens;
import com.hometusk.shared.exception.BusinessException;
import com.hometusk.shared.exception.ErrorCode;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final KeycloakAuthService keycloakAuthService;
    private final AuthCookieService authCookieService;

    public AuthController(KeycloakAuthService keycloakAuthService, AuthCookieService authCookieService) {
        this.keycloakAuthService = keycloakAuthService;
        this.authCookieService = authCookieService;
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest request) {
        AuthTokens tokens = keycloakAuthService.login(request.email(), request.password());
        return withSessionCookies(tokens);
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        AuthTokens tokens = keycloakAuthService.register(request.name(), request.email(), request.password());
        return withSessionCookies(tokens);
    }

    @PostMapping("/mobile/login")
    public ResponseEntity<MobileAuthResponse> mobileLogin(@Valid @RequestBody LoginRequest request) {
        AuthTokens tokens = keycloakAuthService.login(request.email(), request.password());
        return ResponseEntity.ok(MobileAuthResponse.from(tokens));
    }

    @PostMapping("/mobile/register")
    public ResponseEntity<MobileAuthResponse> mobileRegister(@Valid @RequestBody RegisterRequest request) {
        AuthTokens tokens = keycloakAuthService.register(request.name(), request.email(), request.password());
        return ResponseEntity.ok(MobileAuthResponse.from(tokens));
    }

    @PostMapping("/mobile/refresh")
    public ResponseEntity<MobileAuthResponse> mobileRefresh(
            @RequestBody(required = false) MobileRefreshRequest request) {
        String refreshToken = request != null ? request.refreshToken() : null;
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.AUTH_REFRESH_REQUIRED, "Authentication session must be refreshed");
        }

        AuthTokens tokens = keycloakAuthService.refresh(refreshToken);
        return ResponseEntity.ok(MobileAuthResponse.from(tokens));
    }

    @PostMapping("/mobile/logout")
    public ResponseEntity<Void> mobileLogout(@RequestBody(required = false) MobileLogoutRequest request) {
        String refreshToken = request != null ? request.refreshToken() : null;
        keycloakAuthService.logout(refreshToken);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(
            @CookieValue(name = AuthCookieService.REFRESH_COOKIE_NAME, required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.AUTH_REFRESH_REQUIRED, "Authentication session must be refreshed");
        }

        AuthTokens tokens = keycloakAuthService.refresh(refreshToken);
        return withSessionCookies(tokens);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = AuthCookieService.REFRESH_COOKIE_NAME, required = false) String refreshToken) {
        keycloakAuthService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(
                        HttpHeaders.SET_COOKIE,
                        authCookieService.clearAccessCookie().toString())
                .header(
                        HttpHeaders.SET_COOKIE,
                        authCookieService.clearRefreshCookie().toString())
                .build();
    }

    @PostMapping("/session")
    public ResponseEntity<Void> createSession() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = jwtAuth.getToken().getTokenValue();
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        authCookieService
                                .accessCookie(token, jwtAuth.getToken().getExpiresAt())
                                .toString())
                .build();
    }

    private ResponseEntity<Void> withSessionCookies(AuthTokens tokens) {
        return ResponseEntity.noContent()
                .header(
                        HttpHeaders.SET_COOKIE,
                        authCookieService.accessCookie(tokens).toString())
                .header(
                        HttpHeaders.SET_COOKIE,
                        authCookieService.refreshCookie(tokens).toString())
                .build();
    }
}
