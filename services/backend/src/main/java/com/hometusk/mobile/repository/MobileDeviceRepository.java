package com.hometusk.mobile.repository;

import com.hometusk.mobile.domain.MobileDevice;
import com.hometusk.mobile.domain.MobileDeviceStatus;
import com.hometusk.mobile.domain.PushProvider;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MobileDeviceRepository extends JpaRepository<MobileDevice, UUID> {

    Optional<MobileDevice> findByIdAndUser_Id(UUID id, UUID userId);

    Optional<MobileDevice> findByUser_IdAndPushProviderAndPushToken(
            UUID userId, PushProvider pushProvider, String pushToken);

    Optional<MobileDevice> findByPushProviderAndPushTokenAndStatus(
            PushProvider pushProvider, String pushToken, MobileDeviceStatus status);
}
