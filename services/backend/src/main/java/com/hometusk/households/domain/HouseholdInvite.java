package com.hometusk.households.domain;

import com.hometusk.users.domain.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "household_invites",
        indexes = {
            @Index(name = "idx_household_invites_household_id_status", columnList = "household_id,status"),
            @Index(name = "idx_household_invites_invite_token", columnList = "invite_token", unique = true)
        })
public class HouseholdInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdByUser;

    @Column(name = "invite_token", nullable = false, unique = true, length = 128)
    private String inviteToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InviteStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "redeemed_by_user_id")
    private User redeemedByUser;

    @Column(name = "redeemed_at")
    private Instant redeemedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected HouseholdInvite() {}

    public HouseholdInvite(Household household, User createdByUser, String inviteToken, Instant expiresAt) {
        this.household = household;
        this.createdByUser = createdByUser;
        this.inviteToken = inviteToken;
        this.expiresAt = expiresAt;
        this.status = InviteStatus.ACTIVE;
        this.createdAt = Instant.now();
    }

    public void markRedeemed(User redeemedByUser, Instant redeemedAt) {
        this.status = InviteStatus.REDEEMED;
        this.redeemedByUser = redeemedByUser;
        this.redeemedAt = redeemedAt;
    }

    public void markExpired() {
        this.status = InviteStatus.EXPIRED;
    }

    public void markRevoked() {
        this.status = InviteStatus.REVOKED;
    }

    public boolean isExpired(Instant now) {
        return expiresAt.isBefore(now);
    }

    public UUID getId() {
        return id;
    }

    public Household getHousehold() {
        return household;
    }

    public User getCreatedByUser() {
        return createdByUser;
    }

    public String getInviteToken() {
        return inviteToken;
    }

    public InviteStatus getStatus() {
        return status;
    }

    public User getRedeemedByUser() {
        return redeemedByUser;
    }

    public Instant getRedeemedAt() {
        return redeemedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
