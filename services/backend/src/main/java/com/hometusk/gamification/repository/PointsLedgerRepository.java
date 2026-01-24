package com.hometusk.gamification.repository;

import com.hometusk.gamification.domain.PointsLedger;
import com.hometusk.gamification.domain.PointsReason;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PointsLedgerRepository extends JpaRepository<PointsLedger, UUID> {

    Optional<PointsLedger> findByTask_IdAndUser_IdAndReason(UUID taskId, UUID userId, PointsReason reason);

    List<PointsLedger> findByUser_IdAndHousehold_IdOrderByCreatedAtDesc(
            UUID userId, UUID householdId, Pageable pageable);

    long countByUser_IdAndHousehold_IdAndReason(UUID userId, UUID householdId, PointsReason reason);

    long countByUser_IdAndHousehold_IdAndReasonAndCreatedAtAfter(
            UUID userId, UUID householdId, PointsReason reason, Instant since);

    long countByHousehold_IdAndReason(UUID householdId, PointsReason reason);

    @Query("SELECT COALESCE(SUM(p.points), 0) FROM PointsLedger p "
            + "WHERE p.user.id = :userId AND p.household.id = :householdId")
    long sumPointsByUserAndHousehold(@Param("userId") UUID userId, @Param("householdId") UUID householdId);

    @Query("SELECT COALESCE(SUM(p.points), 0) FROM PointsLedger p "
            + "WHERE p.user.id = :userId AND p.household.id = :householdId AND p.createdAt >= :since")
    long sumPointsByUserAndHouseholdSince(
            @Param("userId") UUID userId, @Param("householdId") UUID householdId, @Param("since") Instant since);

    @Query("SELECT COALESCE(SUM(p.points), 0) FROM PointsLedger p WHERE p.household.id = :householdId")
    long sumPointsByHousehold(@Param("householdId") UUID householdId);

    @Query("SELECT t.zone.id, COUNT(p) "
            + "FROM PointsLedger p "
            + "JOIN p.task t "
            + "WHERE p.user.id = :userId "
            + "AND p.household.id = :householdId "
            + "AND p.reason = :reason "
            + "AND t.zone IS NOT NULL "
            + "GROUP BY t.zone.id")
    List<Object[]> countCompletedByZone(
            @Param("userId") UUID userId, @Param("householdId") UUID householdId, @Param("reason") PointsReason reason);
}
