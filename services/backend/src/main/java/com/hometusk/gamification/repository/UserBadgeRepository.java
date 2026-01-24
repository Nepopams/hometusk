package com.hometusk.gamification.repository;

import com.hometusk.gamification.domain.UserBadge;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserBadgeRepository extends JpaRepository<UserBadge, UUID> {

    boolean existsByUser_IdAndHousehold_IdAndBadge_Code(UUID userId, UUID householdId, String badgeCode);

    @Query("SELECT ub.badge.code FROM UserBadge ub WHERE ub.user.id = :userId AND ub.household.id = :householdId")
    List<String> findBadgeCodesByUserAndHousehold(@Param("userId") UUID userId, @Param("householdId") UUID householdId);

    @Query("SELECT ub FROM UserBadge ub JOIN FETCH ub.badge "
            + "WHERE ub.user.id = :userId AND ub.household.id = :householdId "
            + "ORDER BY ub.earnedAt ASC")
    List<UserBadge> findByUserAndHouseholdWithBadge(
            @Param("userId") UUID userId, @Param("householdId") UUID householdId);
}
