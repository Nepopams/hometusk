package com.hometusk.activity.repository;

import com.hometusk.activity.domain.ActivityType;
import com.hometusk.activity.domain.TaskActivity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskActivityRepository extends JpaRepository<TaskActivity, UUID> {

    List<TaskActivity> findByHousehold_IdOrderByCreatedAtDesc(UUID householdId);

    List<TaskActivity> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, UUID entityId);

    List<TaskActivity> findByCorrelationIdOrderByCreatedAtDesc(UUID correlationId);

    List<TaskActivity> findByCommandIdOrderByCreatedAtDesc(UUID commandId);

    List<TaskActivity> findByHousehold_IdAndActivityTypeOrderByCreatedAtDesc(
            UUID householdId, ActivityType activityType);
}
