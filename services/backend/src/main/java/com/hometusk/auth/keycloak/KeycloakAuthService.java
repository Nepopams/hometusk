package com.hometusk.auth.keycloak;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hometusk.auth.service.AuthTokens;
import com.hometusk.shared.exception.BusinessException;
import com.hometusk.shared.exception.ErrorCode;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
public class KeycloakAuthService {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAuthService.class);
    private static final String USER_ROLE = "user";

    private final KeycloakAuthProperties properties;
    private final RestClient restClient;

    public KeycloakAuthService(KeycloakAuthProperties properties) {
        this.properties = properties;

        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeoutMs());
        requestFactory.setReadTimeout(properties.readTimeoutMs());

        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();

        log.info(
                "Keycloak auth client initialized: baseUrl={}, realm={}, userClientId={}, adminClientId={}",
                properties.baseUrl(),
                properties.realm(),
                properties.userClientId(),
                properties.adminClientId());
    }

    public AuthTokens login(String email, String password) {
        MultiValueMap<String, String> form = baseUserClientForm("password");
        form.add("username", normalizeEmail(email));
        form.add("password", password);
        form.add("scope", "openid profile email");

        try {
            return requestToken(form);
        } catch (RestClientResponseException ex) {
            if (isInvalidGrant(ex)) {
                throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS, "Invalid email or password");
            }
            throw authProviderUnavailable("user-token-grant", ex);
        } catch (RestClientException ex) {
            throw authProviderUnavailable("user-token-grant", ex);
        }
    }

    public AuthTokens register(String name, String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        String adminToken = requestAdminToken();

        String userId = createUser(adminToken, name, normalizedEmail, password);
        assignRealmRole(adminToken, userId, USER_ROLE);

        return login(normalizedEmail, password);
    }

    public AuthTokens refresh(String refreshToken) {
        MultiValueMap<String, String> form = baseUserClientForm("refresh_token");
        form.add("refresh_token", refreshToken);

        try {
            return requestToken(form);
        } catch (RestClientResponseException ex) {
            if (isInvalidGrant(ex)) {
                throw new BusinessException(
                        ErrorCode.AUTH_REFRESH_REQUIRED, "Authentication session must be refreshed");
            }
            throw authProviderUnavailable("refresh-token-grant", ex);
        } catch (RestClientException ex) {
            throw authProviderUnavailable("refresh-token-grant", ex);
        }
    }

    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        MultiValueMap<String, String> form = baseUserClientForm(null);
        form.add("refresh_token", refreshToken);

        try {
            restClient
                    .post()
                    .uri(properties.realmPath() + "/protocol/openid-connect/logout")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            log.warn("Keycloak logout failed; clearing local cookies anyway: {}", ex.getMessage());
        }
    }

    private AuthTokens requestToken(MultiValueMap<String, String> form) {
        TokenResponse response = requestTokenResponse(form);

        if (response == null || response.accessToken() == null || response.refreshToken() == null) {
            throw new BusinessException(
                    ErrorCode.AUTH_PROVIDER_UNAVAILABLE, "Authentication provider returned an invalid response");
        }

        return response.toTokens();
    }

    private String requestAdminToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", properties.adminClientId());
        form.add("client_secret", properties.adminClientSecret());

        try {
            TokenResponse response = requestTokenResponse(form);
            if (response == null || response.accessToken() == null) {
                throw new BusinessException(
                        ErrorCode.AUTH_PROVIDER_UNAVAILABLE, "Authentication provider returned an invalid response");
            }
            return response.accessToken();
        } catch (RestClientResponseException ex) {
            throw authProviderUnavailable("admin-token-grant", ex);
        } catch (RestClientException ex) {
            throw authProviderUnavailable("admin-token-grant", ex);
        }
    }

    private TokenResponse requestTokenResponse(MultiValueMap<String, String> form) {
        return restClient
                .post()
                .uri(properties.realmPath() + "/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);
    }

    private String createUser(String adminToken, String name, String email, String password) {
        UserCreateRequest request = UserCreateRequest.from(name, email, password);

        ResponseEntity<Void> response;
        try {
            response = restClient
                    .post()
                    .uri(properties.adminRealmPath() + "/users")
                    .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            if (isStatus(ex, 409)) {
                throw new BusinessException(ErrorCode.AUTH_EMAIL_EXISTS, "An account with this email already exists");
            }
            throw authProviderUnavailable("admin-create-user", ex);
        } catch (RestClientException ex) {
            throw authProviderUnavailable("admin-create-user", ex);
        }

        String userId = extractUserId(response.getHeaders().getLocation());
        if (userId != null) {
            return userId;
        }

        return lookupUserId(adminToken, email);
    }

    private String lookupUserId(String adminToken, String email) {
        KeycloakUser[] users;
        try {
            users = restClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path(properties.adminRealmPath() + "/users")
                            .queryParam("username", email)
                            .queryParam("exact", "true")
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                    .retrieve()
                    .body(KeycloakUser[].class);
        } catch (RestClientResponseException ex) {
            throw authProviderUnavailable("admin-lookup-user", ex);
        } catch (RestClientException ex) {
            throw authProviderUnavailable("admin-lookup-user", ex);
        }

        if (users == null || users.length == 0 || users[0].id() == null) {
            throw new BusinessException(
                    ErrorCode.AUTH_PROVIDER_UNAVAILABLE, "Authentication provider did not return created user");
        }
        return users[0].id();
    }

    private void assignRealmRole(String adminToken, String userId, String roleName) {
        RoleRepresentation role;
        try {
            role = restClient
                    .get()
                    .uri(properties.adminRealmPath() + "/roles/" + roleName)
                    .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                    .retrieve()
                    .body(RoleRepresentation.class);
        } catch (RestClientResponseException ex) {
            throw authProviderUnavailable("admin-get-realm-role", ex);
        } catch (RestClientException ex) {
            throw authProviderUnavailable("admin-get-realm-role", ex);
        }

        if (role == null || role.name() == null) {
            throw new BusinessException(ErrorCode.AUTH_PROVIDER_UNAVAILABLE, "Authentication provider role is missing");
        }

        try {
            restClient
                    .post()
                    .uri(properties.adminRealmPath() + "/users/" + userId + "/role-mappings/realm")
                    .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(List.of(role))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw authProviderUnavailable("admin-assign-realm-role", ex);
        } catch (RestClientException ex) {
            throw authProviderUnavailable("admin-assign-realm-role", ex);
        }
    }

    private MultiValueMap<String, String> baseUserClientForm(String grantType) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        if (grantType != null) {
            form.add("grant_type", grantType);
        }
        form.add("client_id", properties.userClientId());
        return form;
    }

    private BusinessException authProviderUnavailable(String operation, Exception ex) {
        if (ex instanceof RestClientResponseException responseException) {
            log.warn(
                    "Keycloak auth request failed: operation={}, status={}, response={}",
                    operation,
                    responseException.getStatusCode().value(),
                    sanitizeResponse(responseException.getResponseBodyAsString()));
            return new BusinessException(ErrorCode.AUTH_PROVIDER_UNAVAILABLE, "Authentication provider is unavailable");
        }
        if (ex instanceof ResourceAccessException resourceAccessException
                && resourceAccessException.getCause() instanceof SocketTimeoutException) {
            log.warn(
                    "Keycloak auth request timed out: operation={}, message={}",
                    operation,
                    resourceAccessException.getMessage());
            return new BusinessException(ErrorCode.AUTH_PROVIDER_UNAVAILABLE, "Authentication provider timed out");
        }
        log.warn("Keycloak auth request failed: operation={}, message={}", operation, ex.getMessage());
        return new BusinessException(ErrorCode.AUTH_PROVIDER_UNAVAILABLE, "Authentication provider is unavailable");
    }

    private String sanitizeResponse(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "<empty>";
        }
        String compact = responseBody.replaceAll("\\s+", " ").trim();
        return compact.length() <= 500 ? compact : compact.substring(0, 500) + "...";
    }

    private boolean isInvalidGrant(RestClientResponseException ex) {
        return isStatus(ex, 400) || isStatus(ex, 401);
    }

    private boolean isStatus(RestClientResponseException ex, int status) {
        HttpStatusCode statusCode = ex.getStatusCode();
        return statusCode != null && statusCode.value() == status;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String extractUserId(URI location) {
        if (location == null) {
            return null;
        }
        String path = location.getPath();
        int index = path.lastIndexOf('/');
        if (index < 0 || index == path.length() - 1) {
            return null;
        }
        return path.substring(index + 1);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("refresh_token") String refreshToken,
            @JsonProperty("expires_in") long expiresInSeconds,
            @JsonProperty("refresh_expires_in") long refreshExpiresInSeconds) {

        AuthTokens toTokens() {
            return new AuthTokens(accessToken, refreshToken, expiresInSeconds, refreshExpiresInSeconds);
        }
    }

    record UserCreateRequest(
            String username,
            String email,
            boolean enabled,
            boolean emailVerified,
            String firstName,
            List<CredentialRepresentation> credentials) {

        static UserCreateRequest from(String name, String email, String password) {
            String displayName = name == null || name.isBlank() ? email : name.trim();
            return new UserCreateRequest(
                    email,
                    email,
                    true,
                    true,
                    displayName,
                    List.of(new CredentialRepresentation("password", password, false)));
        }
    }

    record CredentialRepresentation(String type, String value, boolean temporary) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KeycloakUser(String id) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RoleRepresentation(
            String id, String name, String description, Boolean composite, Boolean clientRole, String containerId) {}
}
