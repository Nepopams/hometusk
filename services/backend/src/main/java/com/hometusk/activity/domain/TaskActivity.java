package com.hometusk.activity.domain;

import com.hometusk.households.domain.Household;
import com.hometusk.users.domain.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "task_activities")
public class TaskActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;

    @Column(name = "command_id")
    private UUID commandId;

    @Column(name = "correlation_id", nullable = false)
    private UUID correlationId;

    @Column(name = "activity_type", nullable = false)
    private ActivityType activityType;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "changes", columnDefinition = "jsonb")
    private String changes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TaskActivity() {}

    public TaskActivity(
            Household household,
            UUID commandId,
            UUID correlationId,
            ActivityType activityType,
            String entityType,
            UUID entityId,
            User actor) {
        this.household = household;
        this.commandId = commandId;
        this.correlationId = correlationId;
        this.activityType = activityType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.actor = actor;
        this.createdAt = Instant.now();
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

    public UUID getCommandId() {
        return commandId;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public ActivityType getActivityType() {
        return activityType;
    }

    public String getEntityType() {
        return entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public User getActor() {
        return actor;
    }

    @Transient
    public UUID getActorId() {
        return actor.getId();
    }

    public String getChanges() {
        return changes;
    }

    public String getMetadata() {
        return metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    // Setters
    public void setChanges(String changes) {
        this.changes = changes;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}
