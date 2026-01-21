package com.hometusk.notifications.domain;

import com.hometusk.households.domain.Household;
import com.hometusk.users.domain.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "notifications",
        indexes = {
            @Index(
                    name = "idx_notifications_household_user_created_at",
                    columnList = "household_id,user_id,created_at"),
            @Index(name = "idx_notifications_household_user_read_at", columnList = "household_id,user_id,read_at")
        })
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private NotificationType type;

    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "correlation_id")
    private UUID correlationId;

    protected Notification() {}

    public Notification(Household household, User user, NotificationType type, String payloadJson, UUID correlationId) {
        this.household = household;
        this.user = user;
        this.type = type;
        this.payloadJson = payloadJson;
        this.correlationId = correlationId;
        this.createdAt = Instant.now();
    }

    public void markRead(Instant timestamp) {
        if (this.readAt == null) {
            this.readAt = timestamp;
        }
    }

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

    public User getUser() {
        return user;
    }

    @Transient
    public UUID getUserId() {
        return user.getId();
    }

    public NotificationType getType() {
        return type;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }
}
