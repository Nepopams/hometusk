package com.hometusk.gamification.repository;

import com.hometusk.gamification.domain.StreakState;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StreakStateRepository extends JpaRepository<StreakState, UUID> {
    Optional<StreakState> findByUser_IdAndHousehold_Id(UUID userId, UUID householdId);
}
