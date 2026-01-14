package com.hometusk.tasks.repository;

import com.hometusk.tasks.domain.Task;
import com.hometusk.tasks.domain.TaskStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {

    /**
     * Find task by ID scoped to household (IDOR prevention).
     */
    Optional<Task> findByIdAndHouseholdId(UUID id, UUID householdId);

    /**
     * Check if task exists in household.
     */
    boolean existsByIdAndHouseholdId(UUID id, UUID householdId);

    /**
     * List all tasks in a household.
     */
    List<Task> findByHouseholdIdOrderByCreatedAtDesc(UUID householdId);

    /**
     * List tasks by status in a household.
     */
    List<Task> findByHouseholdIdAndStatusOrderByCreatedAtDesc(UUID householdId, TaskStatus status);

    /**
     * List tasks assigned to a user in a household.
     */
    List<Task> findByHouseholdIdAndAssigneeIdOrderByCreatedAtDesc(UUID householdId, UUID assigneeId);

    /**
     * List tasks by status and assignee.
     */
    List<Task> findByHouseholdIdAndStatusAndAssigneeIdOrderByCreatedAtDesc(
            UUID householdId, TaskStatus status, UUID assigneeId);

    /**
     * List tasks by zone in a household.
     */
    List<Task> findByHouseholdIdAndZoneIdOrderByCreatedAtDesc(UUID householdId, UUID zoneId);

    /**
     * List tasks by status and zone.
     */
    List<Task> findByHouseholdIdAndStatusAndZoneIdOrderByCreatedAtDesc(
            UUID householdId, TaskStatus status, UUID zoneId);

    /**
     * List tasks by assignee and zone.
     */
    List<Task> findByHouseholdIdAndAssigneeIdAndZoneIdOrderByCreatedAtDesc(
            UUID householdId, UUID assigneeId, UUID zoneId);

    /**
     * List tasks by status, assignee and zone.
     */
    List<Task> findByHouseholdIdAndStatusAndAssigneeIdAndZoneIdOrderByCreatedAtDesc(
            UUID householdId, TaskStatus status, UUID assigneeId, UUID zoneId);

    /**
     * List open/in-progress tasks for a user across all their households.
     */
    @Query("SELECT t FROM Task t WHERE t.assignee.id = :userId AND t.status IN :statuses ORDER BY t.deadline ASC NULLS LAST, t.createdAt DESC")
    List<Task> findActiveTasksForUser(@Param("userId") UUID userId, @Param("statuses") List<TaskStatus> statuses);

    /**
     * Count tasks by status in a household.
     */
    long countByHouseholdIdAndStatus(UUID householdId, TaskStatus status);

    /**
     * Find tasks created by a specific command.
     */
    List<Task> findByCommandId(UUID commandId);

    /**
     * Count open tasks per assignee in a household (batch query for guardrails).
     * Returns list of [assigneeId, count] pairs.
     */
    @Query(
            "SELECT t.assignee.id, COUNT(t) FROM Task t "
                    + "WHERE t.household.id = :householdId "
                    + "AND t.status IN :statuses "
                    + "AND t.assignee IS NOT NULL "
                    + "GROUP BY t.assignee.id")
    List<Object[]> countTasksByAssigneeAndStatuses(
            @Param("householdId") UUID householdId, @Param("statuses") List<TaskStatus> statuses);
}
