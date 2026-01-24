package com.hometusk.gamification.domain;

import com.hometusk.households.domain.Household;
import com.hometusk.users.domain.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "user_badges",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uq_user_badges_user_household_badge",
                    columnNames = {"user_id", "household_id", "badge_id"})
        })
public class UserBadge {

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
    @JoinColumn(name = "badge_id", nullable = false)
    private Badge badge;

    @Column(name = "earned_at", nullable = false)
    private Instant earnedAt;

    protected UserBadge() {}

    public UserBadge(User user, Household household, Badge badge) {
        this.user = user;
        this.household = household;
        this.badge = badge;
        this.earnedAt = Instant.now();
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

    public Badge getBadge() {
        return badge;
    }

    @Transient
    public UUID getBadgeId() {
        return badge.getId();
    }

    public Instant getEarnedAt() {
        return earnedAt;
    }
}
