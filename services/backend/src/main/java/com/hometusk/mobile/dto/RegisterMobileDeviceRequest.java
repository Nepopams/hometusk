package com.hometusk.mobile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterMobileDeviceRequest(
        @NotBlank @Pattern(regexp = "ios|android", message = "platform must be ios or android") String platform,
        @NotBlank @Pattern(regexp = "expo", message = "pushProvider must be expo") String pushProvider,
        @NotBlank @Size(min = 8, max = 512) String pushToken,
        @Size(max = 120) String deviceName,
        @Size(max = 40) String appVersion,
        @Size(max = 35) String locale,
        @Size(max = 80) String timezone) {}
