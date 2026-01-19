package com.hometusk.commands.pipeline;

import com.hometusk.commands.dto.CompleteTaskPayload;
import com.hometusk.commands.dto.CreateTaskPayload;
import com.hometusk.households.repository.ZoneRepository;
import com.hometusk.shared.exception.BusinessException;
import com.hometusk.shared.exception.ErrorCode;
import com.hometusk.tasks.repository.TaskRepository;
import com.hometusk.users.repository.MembershipRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Validates business rules for commands.
 * Domain invariants are enforced here, not in prompts.
 *
 * Per CLAUDE.md rule 5: Domain invariants over prompts.
 */
@Component
public class BusinessValidator {

    private static final Logger log = LoggerFactory.getLogger(BusinessValidator.class);

    private final MembershipRepository membershipRepository;
    private final ZoneRepository zoneRepository;
    private final TaskRepository taskRepository;

    public BusinessValidator(
            MembershipRepository membershipRepository,
            ZoneRepository zoneRepository,
            TaskRepository taskRepository) {
        this.membershipRepository = membershipRepository;
        this.zoneRepository = zoneRepository;
        this.taskRepository = taskRepository;
    }

    /**
     * Validates create_task payload against business rules.
     *
     * Rules enforced:
     * - Assignee must be member of household (if specified)
     * - Zone must exist in household (if specified)
     * - Deadline must be in the future (if specified)
     */
    public void validateCreateTask(CreateTaskPayload payload, UUID householdId) {
        List<BusinessException.Violation> violations = new ArrayList<>();

        // Rule: Assignee must be member of household
        if (payload.assigneeId() != null) {
            if (!membershipRepository.existsByUserIdAndHouseholdId(payload.assigneeId(), householdId)) {
                violations.add(new BusinessException.Violation(
                        "ASSIGNEE_MUST_BE_MEMBER", "Assignee is not a member of this household"));
            }
        }

        // Rule: Zone must exist in household
        if (payload.zoneId() != null) {
            if (!zoneRepository.existsByIdAndHouseholdId(payload.zoneId(), householdId)) {
                violations.add(
                        new BusinessException.Violation("ZONE_MUST_EXIST", "Zone does not exist in this household"));
            }
        }

        // Rule: Deadline must be in the future
        if (payload.deadline() != null) {
            if (payload.deadline().isBefore(Instant.now())) {
                violations.add(
                        new BusinessException.Violation("DEADLINE_MUST_BE_FUTURE", "Deadline must be in the future"));
            }
        }

        if (!violations.isEmpty()) {
            log.debug("Business validation failed for create_task: {} violations", violations.size());
            throw new BusinessException(
                    ErrorCode.BUSINESS_RULE_VIOLATION,
                    "Business rule violation",
                    violations);
        }

        log.debug("Business validation passed for create_task");
    }

    /**
     * Validates complete_task payload against business rules.
     *
     * Rules enforced:
     * - Task must exist in household
     * - Task must not already be completed
     */
    public void validateCompleteTask(CompleteTaskPayload payload, UUID householdId) {
        List<BusinessException.Violation> violations = new ArrayList<>();

        // Rule: Task must exist in household (IDOR prevention)
        var taskOpt = taskRepository.findByIdAndHouseholdId(payload.taskId(), householdId);
        if (taskOpt.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.TASK_NOT_FOUND,
                    "Task not found in this household",
                    List.of(new BusinessException.Violation("TASK_NOT_FOUND", "Task not found in this household")));
        }

        var task = taskOpt.get();

        // Rule: Task must not already be completed
        if (task.isCompleted()) {
            violations.add(new BusinessException.Violation("TASK_ALREADY_COMPLETED", "Task is already completed"));
        }

        // Rule: Task must not be cancelled
        if (task.isCancelled()) {
            violations.add(
                    new BusinessException.Violation("TASK_CANCELLED", "Cannot complete a cancelled task"));
        }

        if (!violations.isEmpty()) {
            log.debug("Business validation failed for complete_task: {} violations", violations.size());
            throw new BusinessException(
                    ErrorCode.BUSINESS_RULE_VIOLATION,
                    "Business rule violation",
                    violations);
        }

        log.debug("Business validation passed for complete_task");
    }
}
