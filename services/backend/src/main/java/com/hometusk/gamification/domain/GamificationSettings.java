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
import java.util.UUID;

@Entity
@Table(name = "gamification_settings", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "household_id"}))
public class GamificationSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;

    @Column(name = "show_progress_to_others", nullable = false)
    private boolean showProgressToOthers = true;

    @Column(name = "gamification_enabled", nullable = false)
    private boolean gamificationEnabled = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected GamificationSettings() {}

    public GamificationSettings(User user, Household household) {
        this.user = user;
        this.household = household;
        this.showProgressToOthers = true;
        this.gamificationEnabled = true;
        this.createdAt = Instant.now();
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

    public boolean isShowProgressToOthers() {
        return showProgressToOthers;
    }

    public boolean isGamificationEnabled() {
        return gamificationEnabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setShowProgressToOthers(boolean showProgressToOthers) {
        this.showProgressToOthers = showProgressToOthers;
    }

    public void setGamificationEnabled(boolean gamificationEnabled) {
        this.gamificationEnabled = gamificationEnabled;
    }
}
