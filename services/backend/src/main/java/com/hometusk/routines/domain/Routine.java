package com.hometusk.routines.domain;

import com.hometusk.households.domain.Household;
import com.hometusk.households.domain.Zone;
import com.hometusk.users.domain.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "routines")
public class Routine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private Zone zone;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recurrence_rule", nullable = false, columnDefinition = "jsonb")
    private String recurrenceRuleJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_policy", nullable = false, length = 20)
    private AssignmentPolicy assignmentPolicy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fixed_assignee_id")
    private User fixedAssignee;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "round_robin_state", columnDefinition = "jsonb")
    private String roundRobinStateJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RoutineStatus status;

    @Column(name = "generation_window_days", nullable = false)
    private int generationWindowDays;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "paused_at")
    private Instant pausedAt;

    protected Routine() {}

    public Routine(
            Household household,
            String title,
            String recurrenceRuleJson,
            AssignmentPolicy assignmentPolicy,
            User createdBy) {
        this.household = household;
        this.title = title;
        this.recurrenceRuleJson = recurrenceRuleJson;
        this.assignmentPolicy = assignmentPolicy;
        this.createdBy = createdBy;
        this.status = RoutineStatus.ACTIVE;
        this.generationWindowDays = 7;
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

    @Transient
    public UUID getHouseholdId() {
        return household.getId();
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Zone getZone() {
        return zone;
    }

    @Transient
    public UUID getZoneId() {
        return zone != null ? zone.getId() : null;
    }

    public String getRecurrenceRuleJson() {
        return recurrenceRuleJson;
    }

    public AssignmentPolicy getAssignmentPolicy() {
        return assignmentPolicy;
    }

    public User getFixedAssignee() {
        return fixedAssignee;
    }

    @Transient
    public UUID getFixedAssigneeId() {
        return fixedAssignee != null ? fixedAssignee.getId() : null;
    }

    public String getRoundRobinStateJson() {
        return roundRobinStateJson;
    }

    public RoutineStatus getStatus() {
        return status;
    }

    public int getGenerationWindowDays() {
        return generationWindowDays;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    @Transient
    public UUID getCreatedById() {
        return createdBy.getId();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getPausedAt() {
        return pausedAt;
    }

    // Setters
    public void setTitle(String title) {
        this.title = title;
        this.updatedAt = Instant.now();
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = Instant.now();
    }

    public void setZone(Zone zone) {
        this.zone = zone;
        this.updatedAt = Instant.now();
    }

    public void setRecurrenceRuleJson(String recurrenceRuleJson) {
        this.recurrenceRuleJson = recurrenceRuleJson;
        this.updatedAt = Instant.now();
    }

    public void setAssignmentPolicy(AssignmentPolicy assignmentPolicy) {
        this.assignmentPolicy = assignmentPolicy;
        this.updatedAt = Instant.now();
    }

    public void setFixedAssignee(User fixedAssignee) {
        this.fixedAssignee = fixedAssignee;
        this.updatedAt = Instant.now();
    }

    public void setRoundRobinStateJson(String roundRobinStateJson) {
        this.roundRobinStateJson = roundRobinStateJson;
        this.updatedAt = Instant.now();
    }

    public void setStatus(RoutineStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public void setGenerationWindowDays(int generationWindowDays) {
        this.generationWindowDays = generationWindowDays;
        this.updatedAt = Instant.now();
    }

    public void setPausedAt(Instant pausedAt) {
        this.pausedAt = pausedAt;
        this.updatedAt = Instant.now();
    }

    public void softDelete() {
        this.status = RoutineStatus.DELETED;
        this.updatedAt = Instant.now();
    }
}
