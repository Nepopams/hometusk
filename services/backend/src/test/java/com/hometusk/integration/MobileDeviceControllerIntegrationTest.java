package com.hometusk.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hometusk.mobile.domain.MobileDeviceStatus;
import com.hometusk.mobile.dto.RegisterMobileDeviceRequest;
import com.hometusk.mobile.dto.UpdateMobileDeviceRequest;
import com.hometusk.mobile.repository.MobileDeviceRepository;
import com.hometusk.users.domain.User;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

@DisplayName("Mobile Device Controller Integration Tests")
class MobileDeviceControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MobileDeviceRepository mobileDeviceRepository;

    @Test
    @DisplayName("Register returns device registration without exposing push token")
    void registerMobileDevice_returnsDeviceWithoutToken() throws Exception {
        String token = expoToken("register");

        mockMvc.perform(post("/api/v1/mobile/devices")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest("android", token))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.userId").value(testUser.getId().toString()))
                .andExpect(jsonPath("$.platform").value("android"))
                .andExpect(jsonPath("$.pushProvider").value("expo"))
                .andExpect(jsonPath("$.status").value("active"))
                .andExpect(jsonPath("$.deviceName").value("Pixel 8"))
                .andExpect(jsonPath("$.appVersion").value("0.1.0"))
                .andExpect(jsonPath("$.pushToken").doesNotExist());

        var stored = mobileDeviceRepository.findAll();
        assertThat(stored).hasSize(1);
        assertThat(stored.get(0).getPushToken()).isEqualTo(token);
    }

    @Test
    @DisplayName("Register refreshes same user's existing provider token")
    void registerMobileDevice_withSameUserAndToken_refreshesExistingRegistration() throws Exception {
        String token = expoToken("refresh");
        UUID firstId = registerDevice(testUser, registerRequest("ios", token));

        MvcResult result = mockMvc.perform(post("/api/v1/mobile/devices")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterMobileDeviceRequest(
                                "ios", "expo", token, "iPhone 15", "0.2.0", "en-US", "Europe/Moscow"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(firstId.toString()))
                .andExpect(jsonPath("$.deviceName").value("iPhone 15"))
                .andExpect(jsonPath("$.appVersion").value("0.2.0"))
                .andReturn();

        UUID secondId = responseId(result);
        assertThat(secondId).isEqualTo(firstId);
        assertThat(mobileDeviceRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Register rejects token already active for another user")
    void registerMobileDevice_withAnotherUsersActiveToken_returns409() throws Exception {
        String token = expoToken("conflict");
        registerDevice(testUser, registerRequest("android", token));

        mockMvc.perform(post("/api/v1/mobile/devices")
                        .with(jwtForUser(testUser2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest("android", token))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DEVICE_TOKEN_CONFLICT"));
    }

    @Test
    @DisplayName("Patch rotates token and updates status metadata for current user's device")
    void updateMobileDevice_rotatesTokenAndStatus() throws Exception {
        UUID deviceId = registerDevice(testUser, registerRequest("android", expoToken("old")));
        String nextToken = expoToken("new");

        mockMvc.perform(patch("/api/v1/mobile/devices/{deviceId}", deviceId)
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateMobileDeviceRequest(nextToken, "inactive", "0.3.0", "ru-RU", "UTC"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(deviceId.toString()))
                .andExpect(jsonPath("$.status").value("inactive"))
                .andExpect(jsonPath("$.appVersion").value("0.3.0"))
                .andExpect(jsonPath("$.pushToken").doesNotExist());

        var device = mobileDeviceRepository.findById(deviceId).orElseThrow();
        assertThat(device.getPushToken()).isEqualTo(nextToken);
        assertThat(device.getStatus()).isEqualTo(MobileDeviceStatus.INACTIVE);
        assertThat(device.getLocale()).isEqualTo("ru-RU");
    }

    @Test
    @DisplayName("Patch rejects cross-user device access as not found")
    void updateMobileDevice_forAnotherUser_returns404() throws Exception {
        UUID deviceId = registerDevice(testUser, registerRequest("android", expoToken("cross-user-patch")));

        mockMvc.perform(patch("/api/v1/mobile/devices/{deviceId}", deviceId)
                        .with(jwtForUser(testUser2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateMobileDeviceRequest(null, "inactive", null, null, null))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("DEVICE_NOT_FOUND"));
    }

    @Test
    @DisplayName("Delete deactivates current user's device registration")
    void deleteMobileDevice_deactivatesCurrentUsersDevice() throws Exception {
        UUID deviceId = registerDevice(testUser, registerRequest("ios", expoToken("delete")));

        mockMvc.perform(delete("/api/v1/mobile/devices/{deviceId}", deviceId).with(jwt()))
                .andExpect(status().isNoContent());

        var device = mobileDeviceRepository.findById(deviceId).orElseThrow();
        assertThat(device.getStatus()).isEqualTo(MobileDeviceStatus.INACTIVE);
    }

    @Test
    @DisplayName("Delete rejects cross-user device access as not found")
    void deleteMobileDevice_forAnotherUser_returns404() throws Exception {
        UUID deviceId = registerDevice(testUser, registerRequest("android", expoToken("cross-user-delete")));

        mockMvc.perform(delete("/api/v1/mobile/devices/{deviceId}", deviceId).with(jwtForUser(testUser2)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("DEVICE_NOT_FOUND"));
    }

    private UUID registerDevice(User user, RegisterMobileDeviceRequest request) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/mobile/devices")
                        .with(jwtForUser(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return responseId(result);
    }

    private UUID responseId(MvcResult result) throws Exception {
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id")
                .asText());
    }

    private RegisterMobileDeviceRequest registerRequest(String platform, String token) {
        return new RegisterMobileDeviceRequest(
                platform, "expo", token, "Pixel 8", "0.1.0", "en-US", "Europe/Moscow");
    }

    private String expoToken(String label) {
        return "ExponentPushToken[" + label + "-" + UUID.randomUUID() + "]";
    }
}
