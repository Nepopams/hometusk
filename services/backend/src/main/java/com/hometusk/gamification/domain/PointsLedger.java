package com.hometusk.gamification.domain;

import com.hometusk.households.domain.Household;
import com.hometusk.tasks.domain.Task;
import com.hometusk.users.domain.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "points_ledger",
        indexes = {
            @Index(name = "idx_points_ledger_user_household_created", columnList = "user_id,household_id,created_at"),
            @Index(name = "idx_points_ledger_household_created", columnList = "household_id,created_at")
        },
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "idx_points_ledger_task_user_reason",
                    columnNames = {"task_id", "user_id", "reason"})
        })
public class PointsLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private Task task;

    @Column(name = "points", nullable = false)
    private int points;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 50)
    private PointsReason reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "note", length = 500)
    private String note;

    protected PointsLedger() {}

    public PointsLedger(User user, Household household, Task task, int points, PointsReason reason) {
        this(user, household, task, points, reason, null, null);
    }

    public PointsLedger(
            User user, Household household, Task task, int points, PointsReason reason, User createdBy, String note) {
        this.user = user;
        this.household = household;
        this.task = task;
        this.points = points;
        this.reason = reason;
        this.createdBy = createdBy;
        this.note = note;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    @Transient
    public UUID getUserId() {
        return user.getId();
    }

    public Household getHousehold() {
        return household;
    }

    @Transient
    public UUID getHouseholdId() {
        return household.getId();
    }

    public Task getTask() {
        return task;
    }

    @Transient
    public UUID getTaskId() {
        return task != null ? task.getId() : null;
    }

    public int getPoints() {
        return points;
    }

    public PointsReason getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    @Transient
    public UUID getCreatedById() {
        return createdBy != null ? createdBy.getId() : null;
    }

    public String getNote() {
        return note;
    }
}
