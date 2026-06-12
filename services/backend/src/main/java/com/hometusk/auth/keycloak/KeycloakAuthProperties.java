package com.hometusk.auth.keycloak;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "hometusk.auth.keycloak")
public record KeycloakAuthProperties(
        @NotBlank String baseUrl,
        @NotBlank String realm,
        @NotBlank String userClientId,
        @NotBlank String adminClientId,
        @NotBlank String adminClientSecret,
        @Positive int connectTimeoutMs,
        @Positive int readTimeoutMs) {

    public String realmPath() {
        return "/realms/" + realm;
    }

    public String adminRealmPath() {
        return "/admin/realms/" + realm;
    }
}
