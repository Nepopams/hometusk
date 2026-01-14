package com.hometusk.tasks.service;

import com.hometusk.households.domain.Household;
import com.hometusk.households.domain.Zone;
import com.hometusk.shared.exception.BusinessException;
import com.hometusk.shared.exception.ErrorCode;
import com.hometusk.shared.exception.NotFoundException;
import com.hometusk.tasks.domain.Task;
import com.hometusk.tasks.domain.TaskStatus;
import com.hometusk.tasks.repository.TaskRepository;
import com.hometusk.users.domain.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Transactional
    public Task create(CreateTaskRequest request) {
        log.debug(
                "Creating task: title={}, householdId={}, createdById={}",
                request.title(),
                request.household().getId(),
                request.createdBy().getId());

        Task task = new Task(request.household(), request.title(), request.createdBy());

        if (request.description() != null) {
            task.setDescription(request.description());
        }

        if (request.assignee() != null) {
            task.setAssignee(request.assignee());
        }

        if (request.zone() != null) {
            task.setZone(request.zone());
        }

        if (request.deadline() != null) {
            task.setDeadline(request.deadline());
        }

        if (request.commandId() != null) {
            task.setCommandId(request.commandId());
        }

        if (request.createdVia() != null) {
            task.setCreatedVia(request.createdVia());
        }

        Task saved = taskRepository.save(task);
        log.info("Task created: id={}, householdId={}", saved.getId(), saved.getHouseholdId());

        return saved;
    }

    @Transactional
    public Task complete(UUID taskId, UUID householdId) {
        Task task = getByIdAndHouseholdId(taskId, householdId);

        if (task.isCompleted()) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_RULE_VIOLATION,
                    "Task is already completed",
                    BusinessException.violation("TASK_ALREADY_COMPLETED", "Task is already completed"));
        }

        if (task.isCancelled()) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_RULE_VIOLATION,
                    "Cannot complete cancelled task",
                    BusinessException.violation("TASK_CANCELLED", "Cannot complete a cancelled task"));
        }

        task.complete();
        Task saved = taskRepository.save(task);

        log.info("Task completed: id={}, householdId={}", saved.getId(), householdId);
        return saved;
    }

    @Transactional
    public Task cancel(UUID taskId, UUID householdId) {
        Task task = getByIdAndHouseholdId(taskId, householdId);
        task.cancel();
        return taskRepository.save(task);
    }

    @Transactional
    public Task assign(UUID taskId, UUID householdId, User assignee) {
        Task task = getByIdAndHouseholdId(taskId, householdId);
        task.setAssignee(assignee);
        return taskRepository.save(task);
    }

    @Transactional(readOnly = true)
    public Task getByIdAndHouseholdId(UUID taskId, UUID householdId) {
        return taskRepository
                .findByIdAndHouseholdId(taskId, householdId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.TASK_NOT_FOUND, "Task not found: " + taskId));
    }

    @Transactional(readOnly = true)
    public Optional<Task> findByIdAndHouseholdId(UUID taskId, UUID householdId) {
        return taskRepository.findByIdAndHouseholdId(taskId, householdId);
    }

    @Transactional(readOnly = true)
    public boolean existsInHousehold(UUID taskId, UUID householdId) {
        return taskRepository.existsByIdAndHouseholdId(taskId, householdId);
    }

    @Transactional(readOnly = true)
    public List<Task> findByHouseholdId(UUID householdId, TaskStatus status, UUID assigneeId) {
        return findByHouseholdId(householdId, status, assigneeId, null);
    }

    @Transactional(readOnly = true)
    public List<Task> findByHouseholdId(UUID householdId, TaskStatus status, UUID assigneeId, UUID zoneId) {
        // All three filters
        if (status != null && assigneeId != null && zoneId != null) {
            return taskRepository.findByHouseholdIdAndStatusAndAssigneeIdAndZoneIdOrderByCreatedAtDesc(
                    householdId, status, assigneeId, zoneId);
        }
        // Two filters
        if (status != null && assigneeId != null) {
            return taskRepository.findByHouseholdIdAndStatusAndAssigneeIdOrderByCreatedAtDesc(
                    householdId, status, assigneeId);
        }
        if (status != null && zoneId != null) {
            return taskRepository.findByHouseholdIdAndStatusAndZoneIdOrderByCreatedAtDesc(
                    householdId, status, zoneId);
        }
        if (assigneeId != null && zoneId != null) {
            return taskRepository.findByHouseholdIdAndAssigneeIdAndZoneIdOrderByCreatedAtDesc(
                    householdId, assigneeId, zoneId);
        }
        // Single filter
        if (status != null) {
            return taskRepository.findByHouseholdIdAndStatusOrderByCreatedAtDesc(householdId, status);
        }
        if (assigneeId != null) {
            return taskRepository.findByHouseholdIdAndAssigneeIdOrderByCreatedAtDesc(householdId, assigneeId);
        }
        if (zoneId != null) {
            return taskRepository.findByHouseholdIdAndZoneIdOrderByCreatedAtDesc(householdId, zoneId);
        }
        // No filters
        return taskRepository.findByHouseholdIdOrderByCreatedAtDesc(householdId);
    }

    public record CreateTaskRequest(
            Household household,
            String title,
            String description,
            User createdBy,
            User assignee,
            Zone zone,
            Instant deadline,
            UUID commandId,
            String createdVia) {

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private Household household;
            private String title;
            private String description;
            private User createdBy;
            private User assignee;
            private Zone zone;
            private Instant deadline;
            private UUID commandId;
            private String createdVia = "command";

            public Builder household(Household household) {
                this.household = household;
                return this;
            }

            public Builder title(String title) {
                this.title = title;
                return this;
            }

            public Builder description(String description) {
                this.description = description;
                return this;
            }

            public Builder createdBy(User createdBy) {
                this.createdBy = createdBy;
                return this;
            }

            public Builder assignee(User assignee) {
                this.assignee = assignee;
                return this;
            }

            public Builder zone(Zone zone) {
                this.zone = zone;
                return this;
            }

            public Builder deadline(Instant deadline) {
                this.deadline = deadline;
                return this;
            }

            public Builder commandId(UUID commandId) {
                this.commandId = commandId;
                return this;
            }

            public Builder createdVia(String createdVia) {
                this.createdVia = createdVia;
                return this;
            }

            public CreateTaskRequest build() {
                return new CreateTaskRequest(
                        household, title, description, createdBy, assignee, zone, deadline, commandId, createdVia);
            }
        }
    }
}
