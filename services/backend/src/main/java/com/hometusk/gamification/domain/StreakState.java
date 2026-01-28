package com.hometusk.gamification.domain;

import com.hometusk.households.domain.Household;
import com.hometusk.users.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "streak_states", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "household_id"}))
public class StreakState {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;

    @Column(name = "current_streak", nullable = false)
    private int currentStreak = 0;

    @Column(name = "best_streak", nullable = false)
    private int bestStreak = 0;

    @Column(name = "last_activity_date")
    private LocalDate lastActivityDate;

    @Column(name = "grace_used_today", nullable = false)
    private boolean graceUsedToday = false;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StreakState() {}

    public StreakState(User user, Household household) {
        this.user = user;
        this.household = household;
        this.currentStreak = 0;
        this.bestStreak = 0;
        this.graceUsedToday = false;
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Household getHousehold() {
        return household;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public int getBestStreak() {
        return bestStreak;
    }

    public LocalDate getLastActivityDate() {
        return lastActivityDate;
    }

    public boolean isGraceUsedToday() {
        return graceUsedToday;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
    }

    public void setBestStreak(int bestStreak) {
        this.bestStreak = bestStreak;
    }

    public void setLastActivityDate(LocalDate lastActivityDate) {
        this.lastActivityDate = lastActivityDate;
    }

    public void setGraceUsedToday(boolean graceUsedToday) {
        this.graceUsedToday = graceUsedToday;
    }
}
