package com.hometusk.mobile.service;

import com.hometusk.mobile.domain.MobileDevice;
import com.hometusk.mobile.domain.MobileDeviceStatus;
import com.hometusk.mobile.domain.MobilePlatform;
import com.hometusk.mobile.domain.PushProvider;
import com.hometusk.mobile.dto.RegisterMobileDeviceRequest;
import com.hometusk.mobile.dto.UpdateMobileDeviceRequest;
import com.hometusk.mobile.repository.MobileDeviceRepository;
import com.hometusk.shared.exception.BusinessException;
import com.hometusk.shared.exception.ErrorCode;
import com.hometusk.shared.exception.NotFoundException;
import com.hometusk.users.domain.User;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MobileDeviceService {

    private final MobileDeviceRepository mobileDeviceRepository;

    public MobileDeviceService(MobileDeviceRepository mobileDeviceRepository) {
        this.mobileDeviceRepository = mobileDeviceRepository;
    }

    @Transactional
    public MobileDevice register(User user, RegisterMobileDeviceRequest request) {
        MobilePlatform platform = parsePlatform(request.platform());
        PushProvider provider = parseProvider(request.pushProvider());
        String token = normalizeToken(request.pushToken());

        return mobileDeviceRepository
                .findByUser_IdAndPushProviderAndPushToken(user.getId(), provider, token)
                .map(device -> {
                    assertTokenAvailableForDevice(device.getId(), user.getId(), provider, token);
                    device.refresh(
                            platform,
                            trimToNull(request.deviceName()),
                            trimToNull(request.appVersion()),
                            trimToNull(request.locale()),
                            trimToNull(request.timezone()));
                    return mobileDeviceRepository.save(device);
                })
                .orElseGet(() -> {
                    assertTokenAvailableForDevice(null, user.getId(), provider, token);
                    MobileDevice device = new MobileDevice(user, platform, provider, token);
                    device.refresh(
                            platform,
                            trimToNull(request.deviceName()),
                            trimToNull(request.appVersion()),
                            trimToNull(request.locale()),
                            trimToNull(request.timezone()));
                    return mobileDeviceRepository.save(device);
                });
    }

    @Transactional
    public MobileDevice update(UUID userId, UUID deviceId, UpdateMobileDeviceRequest request) {
        MobileDevice device = mobileDeviceRepository
                .findByIdAndUser_Id(deviceId, userId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.DEVICE_NOT_FOUND, "Mobile device registration not found"));

        String nextToken = trimToNull(request.pushToken());
        if (nextToken != null) {
            nextToken = normalizeToken(nextToken);
            assertTokenAvailableForDevice(device.getId(), userId, device.getPushProvider(), nextToken);
            device.updateToken(nextToken);
        }

        if (request.status() != null) {
            device.updateStatus(parseStatus(request.status()));
        }
        device.updateMetadata(
                trimToNull(request.appVersion()), trimToNull(request.locale()), trimToNull(request.timezone()));

        return mobileDeviceRepository.save(device);
    }

    @Transactional
    public void deactivate(UUID userId, UUID deviceId) {
        MobileDevice device = mobileDeviceRepository
                .findByIdAndUser_Id(deviceId, userId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.DEVICE_NOT_FOUND, "Mobile device registration not found"));
        device.deactivate();
        mobileDeviceRepository.save(device);
    }

    private void assertTokenAvailableForDevice(
            UUID currentDeviceId, UUID userId, PushProvider provider, String token) {
        mobileDeviceRepository
                .findByPushProviderAndPushTokenAndStatus(provider, token, MobileDeviceStatus.ACTIVE)
                .filter(existing -> !existing.getId().equals(currentDeviceId))
                .ifPresent(existing -> {
                    throw new BusinessException(
                            ErrorCode.DEVICE_TOKEN_CONFLICT,
                            existing.getUserId().equals(userId)
                                    ? "Mobile push token already belongs to another active registration"
                                    : "Mobile push token belongs to another active user registration");
                });
    }

    private MobilePlatform parsePlatform(String value) {
        return MobilePlatform.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    private PushProvider parseProvider(String value) {
        return PushProvider.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    private MobileDeviceStatus parseStatus(String value) {
        return MobileDeviceStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    private String normalizeToken(String value) {
        return value.trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
