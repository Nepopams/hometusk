package com.hometusk.tasks.domain;

import com.hometusk.households.domain.Household;
import com.hometusk.households.domain.Zone;
import com.hometusk.users.domain.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TaskStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private Zone zone;

    @Column(name = "deadline")
    private Instant deadline;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Column(name = "command_id")
    private UUID commandId;

    @Column(name = "created_via", nullable = false, length = 50)
    private String createdVia;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected Task() {}

    public Task(Household household, String title, User createdBy) {
        this.household = household;
        this.title = title;
        this.createdBy = createdBy;
        this.status = TaskStatus.OPEN;
        this.createdVia = "command";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public Household getHousehold() {
        return household;
    }

    public UUID getHouseholdId() {
        return household.getId();
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public User getAssignee() {
        return assignee;
    }

    public UUID getAssigneeId() {
        return assignee != null ? assignee.getId() : null;
    }

    public Zone getZone() {
        return zone;
    }

    public UUID getZoneId() {
        return zone != null ? zone.getId() : null;
    }

    public Instant getDeadline() {
        return deadline;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public UUID getCreatedById() {
        return createdBy.getId();
    }

    public UUID getCommandId() {
        return commandId;
    }

    public String getCreatedVia() {
        return createdVia;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    // Setters for mutable fields
    public void setTitle(String title) {
        this.title = title;
        this.updatedAt = Instant.now();
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = Instant.now();
    }

    public void setAssignee(User assignee) {
        this.assignee = assignee;
        this.updatedAt = Instant.now();
    }

    public void setZone(Zone zone) {
        this.zone = zone;
        this.updatedAt = Instant.now();
    }

    public void setDeadline(Instant deadline) {
        this.deadline = deadline;
        this.updatedAt = Instant.now();
    }

    public void setCommandId(UUID commandId) {
        this.commandId = commandId;
    }

    public void setCreatedVia(String createdVia) {
        this.createdVia = createdVia;
    }

    // Status transitions
    public void start() {
        if (this.status != TaskStatus.OPEN) {
            throw new IllegalStateException("Cannot start task in status: " + this.status);
        }
        this.status = TaskStatus.IN_PROGRESS;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        if (this.status == TaskStatus.DONE) {
            throw new IllegalStateException("Task is already completed");
        }
        if (this.status == TaskStatus.CANCELLED) {
            throw new IllegalStateException("Cannot complete cancelled task");
        }
        this.status = TaskStatus.DONE;
        this.completedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void cancel() {
        if (this.status == TaskStatus.DONE) {
            throw new IllegalStateException("Cannot cancel completed task");
        }
        if (this.status == TaskStatus.CANCELLED) {
            return; // Already cancelled
        }
        this.status = TaskStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

    public void reopen() {
        if (this.status != TaskStatus.CANCELLED && this.status != TaskStatus.DONE) {
            throw new IllegalStateException("Can only reopen cancelled or done tasks");
        }
        this.status = TaskStatus.OPEN;
        this.completedAt = null;
        this.updatedAt = Instant.now();
    }

    public boolean isCompleted() {
        return this.status == TaskStatus.DONE;
    }

    public boolean isCancelled() {
        return this.status == TaskStatus.CANCELLED;
    }

    public boolean isOpen() {
        return this.status == TaskStatus.OPEN;
    }
}
