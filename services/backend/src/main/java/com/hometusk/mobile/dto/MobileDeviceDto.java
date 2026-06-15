package com.hometusk.mobile.dto;

import com.hometusk.mobile.domain.MobileDevice;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public record MobileDeviceDto(
        UUID id,
        UUID userId,
        String platform,
        String pushProvider,
        String status,
        String deviceName,
        String appVersion,
        String locale,
        String timezone,
        Instant lastSeenAt,
        Instant createdAt,
        Instant updatedAt) {

    public static MobileDeviceDto from(MobileDevice device) {
        return new MobileDeviceDto(
                device.getId(),
                device.getUserId(),
                lower(device.getPlatform().name()),
                lower(device.getPushProvider().name()),
                lower(device.getStatus().name()),
                device.getDeviceName(),
                device.getAppVersion(),
                device.getLocale(),
                device.getTimezone(),
                device.getLastSeenAt(),
                device.getCreatedAt(),
                device.getUpdatedAt());
    }

    private static String lower(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
