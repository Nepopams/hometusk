package com.hometusk.routines.repository;

import com.hometusk.routines.domain.AssignmentPolicy;
import com.hometusk.routines.domain.Routine;
import com.hometusk.routines.domain.RoutineStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RoutineRepository extends JpaRepository<Routine, UUID> {

    List<Routine> findByStatus(RoutineStatus status);

    List<Routine> findByHousehold_IdAndStatusInOrderByCreatedAtDesc(UUID householdId, List<RoutineStatus> statuses);

    List<Routine> findByHousehold_IdAndStatusOrderByCreatedAtDesc(UUID householdId, RoutineStatus status);

    List<Routine> findByHousehold_IdAndStatusInAndAssignmentPolicyOrderByCreatedAtDesc(
            UUID householdId, List<RoutineStatus> statuses, AssignmentPolicy assignmentPolicy);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Routine r where r.id = :id")
    Optional<Routine> findByIdForUpdate(@Param("id") UUID id);

    Optional<Routine> findByIdAndHousehold_Id(UUID id, UUID householdId);

    boolean existsByIdAndHousehold_Id(UUID id, UUID householdId);
}
