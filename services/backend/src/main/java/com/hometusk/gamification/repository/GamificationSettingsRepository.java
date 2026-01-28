package com.hometusk.gamification.repository;

import com.hometusk.gamification.domain.GamificationSettings;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GamificationSettingsRepository extends JpaRepository<GamificationSettings, UUID> {
    Optional<GamificationSettings> findByUser_IdAndHousehold_Id(UUID userId, UUID householdId);
}
