package com.hometusk.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hometusk.users.domain.User;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@DisplayName("Notification SSE Integration Tests")
@Import(NotificationSseIntegrationTest.TestJwtDecoderConfig.class)
class NotificationSseIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("Create session sets HttpOnly cookie")
    void createSession_withValidJwt_shouldSetCookie() throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/api/v1/auth/session").with(jwtForUserWithToken(testUser, "user1-token")))
                .andExpect(status().isOk())
                .andReturn();

        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).contains("hometusk_token=user1-token");
    }

    @Test
    @Disabled("TODO: Flaky SSE async test - needs investigation")
    @DisplayName("SSE stream connects with valid session cookie")
    void streamNotifications_withValidSession_shouldConnect() throws Exception {
        Cookie cookie = createSessionCookieForUser(testUser, "user1-token");

        MvcResult result = mockMvc.perform(get("/api/v1/households/{id}/notifications/stream", testHousehold.getId())
                        .cookie(cookie))
                .andExpect(request().asyncStarted())
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("SSE stream requires authentication")
    void streamNotifications_withoutSession_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/households/{id}/notifications/stream", testHousehold.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("SSE stream enforces household membership")
    void streamNotifications_notMember_shouldReturn403() throws Exception {
        Cookie cookie = createSessionCookieForUser(testUser2, "user2-token");

        mockMvc.perform(get("/api/v1/households/{id}/notifications/stream", testHousehold.getId())
                        .cookie(cookie))
                .andExpect(status().isForbidden());
    }

    private Cookie createSessionCookieForUser(User user, String tokenValue) throws Exception {
        mockMvc.perform(post("/api/v1/auth/session").with(jwtForUserWithToken(user, tokenValue)))
                .andExpect(status().isOk())
                .andReturn();

        return new Cookie("hometusk_token", tokenValue);
    }

    private RequestPostProcessor jwtForUserWithToken(User user, String tokenValue) {
        Instant now = Instant.now();
        return SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.tokenValue(tokenValue)
                .subject(user.getExternalId())
                .claim("email", user.getEmail())
                .claim("name", user.getDisplayName())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600)));
    }

    @TestConfiguration
    static class TestJwtDecoderConfig {

        @Bean
        @Primary
        JwtDecoder jwtDecoder() {
            // Use dynamic external IDs from test users by extracting from token
            return token -> {
                // The token value contains the user identifier, lookup happens via SecurityContext
                Instant now = Instant.now();
                return Jwt.withTokenValue(token)
                        .header("alg", "none")
                        .subject(token) // Subject will be matched via SecurityContext, not DB lookup
                        .claim("email", token + "@test.local")
                        .claim("name", "Test User")
                        .issuedAt(now)
                        .expiresAt(now.plusSeconds(3600))
                        .build();
            };
        }
    }
}
