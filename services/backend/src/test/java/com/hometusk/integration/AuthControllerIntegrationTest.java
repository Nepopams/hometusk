package com.hometusk.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hometusk.auth.api.AuthController;
import com.hometusk.auth.dto.LoginRequest;
import com.hometusk.auth.dto.RegisterRequest;
import com.hometusk.auth.filter.JwtCookieAuthFilter;
import com.hometusk.auth.keycloak.KeycloakAuthService;
import com.hometusk.auth.service.AuthCookieService;
import com.hometusk.auth.service.AuthTokens;
import com.hometusk.config.SecurityConfig;
import com.hometusk.shared.exception.BusinessException;
import com.hometusk.shared.exception.ErrorCode;
import com.hometusk.shared.exception.GlobalExceptionHandler;
import jakarta.servlet.http.Cookie;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;

@DisplayName("Auth Controller Integration Tests")
@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, JwtCookieAuthFilter.class, AuthCookieService.class, GlobalExceptionHandler.class})
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KeycloakAuthService keycloakAuthService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    @DisplayName("Login sets HttpOnly access and refresh cookies")
    void login_withValidCredentials_setsSessionCookies() throws Exception {
        when(keycloakAuthService.login(anyString(), anyString())).thenReturn(tokens());

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("alice@test.local", "password123"))))
                .andExpect(status().isNoContent())
                .andReturn();

        List<String> cookies = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
        assertThat(cookies).anySatisfy(cookie -> {
            assertThat(cookie).contains("hometusk_token=access-token");
            assertThat(cookie).contains("HttpOnly");
            assertThat(cookie).contains("Path=/");
        });
        assertThat(cookies).anySatisfy(cookie -> {
            assertThat(cookie).contains("hometusk_refresh_token=refresh-token");
            assertThat(cookie).contains("HttpOnly");
            assertThat(cookie).contains("Path=/api/v1/auth");
        });
    }

    @Test
    @DisplayName("Login maps invalid credentials to 401")
    void login_withInvalidCredentials_returns401() throws Exception {
        when(keycloakAuthService.login(anyString(), anyString()))
                .thenThrow(new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS, "Invalid email or password"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("alice@test.local", "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH_INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("Register sets cookies after creating Keycloak user")
    void register_withValidPayload_setsSessionCookies() throws Exception {
        when(keycloakAuthService.register(anyString(), anyString(), anyString()))
                .thenReturn(tokens());

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("Alice Test", "alice@test.local", "password123"))))
                .andExpect(status().isNoContent())
                .andReturn();

        List<String> cookies = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
        assertThat(cookies).anyMatch(cookie -> cookie.contains("hometusk_token=access-token"));
        assertThat(cookies).anyMatch(cookie -> cookie.contains("hometusk_refresh_token=refresh-token"));
    }

    @Test
    @DisplayName("Register maps duplicate email to 409")
    void register_withDuplicateEmail_returns409() throws Exception {
        when(keycloakAuthService.register(anyString(), anyString(), anyString()))
                .thenThrow(new BusinessException(
                        ErrorCode.AUTH_EMAIL_EXISTS, "An account with this email already exists"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("Alice Test", "alice@test.local", "password123"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("AUTH_EMAIL_EXISTS"));
    }

    @Test
    @DisplayName("Refresh requires refresh cookie")
    void refresh_withoutRefreshCookie_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH_REFRESH_REQUIRED"));
    }

    @Test
    @DisplayName("Refresh updates session cookies")
    void refresh_withValidRefreshCookie_setsSessionCookies() throws Exception {
        when(keycloakAuthService.refresh("old-refresh-token")).thenReturn(tokens());

        MvcResult result = mockMvc.perform(
                        post("/api/v1/auth/refresh").cookie(new Cookie("hometusk_refresh_token", "old-refresh-token")))
                .andExpect(status().isNoContent())
                .andReturn();

        List<String> cookies = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
        assertThat(cookies).anyMatch(cookie -> cookie.contains("hometusk_token=access-token"));
        assertThat(cookies).anyMatch(cookie -> cookie.contains("hometusk_refresh_token=refresh-token"));
    }

    @Test
    @DisplayName("Logout clears access and refresh cookies")
    void logout_clearsSessionCookies() throws Exception {
        doNothing().when(keycloakAuthService).logout("old-refresh-token");

        MvcResult result = mockMvc.perform(
                        post("/api/v1/auth/logout").cookie(new Cookie("hometusk_refresh_token", "old-refresh-token")))
                .andExpect(status().isNoContent())
                .andReturn();

        List<String> cookies = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
        assertThat(cookies).anySatisfy(cookie -> {
            assertThat(cookie).contains("hometusk_token=");
            assertThat(cookie).contains("Max-Age=0");
        });
        assertThat(cookies).anySatisfy(cookie -> {
            assertThat(cookie).contains("hometusk_refresh_token=");
            assertThat(cookie).contains("Max-Age=0");
        });
    }

    @Test
    @DisplayName("Create session keeps legacy bearer-token bridge")
    void createSession_withBearerJwt_setsAccessCookie() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/session")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.tokenValue("legacy-token"))))
                .andExpect(status().isOk())
                .andReturn();

        List<String> cookies = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
        assertThat(cookies).anyMatch(cookie -> cookie.contains("hometusk_token=legacy-token"));
    }

    private AuthTokens tokens() {
        return new AuthTokens("access-token", "refresh-token", 3600, 7200);
    }
}
