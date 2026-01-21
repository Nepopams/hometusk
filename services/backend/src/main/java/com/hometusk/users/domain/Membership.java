package com.hometusk.users.domain;

import com.hometusk.households.domain.Household;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "memberships")
public class Membership {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private MembershipRole role;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    protected Membership() {}

    public Membership(User user, Household household, MembershipRole role) {
        this.user = user;
        this.household = household;
        this.role = role;
        this.joinedAt = Instant.now();
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

    public MembershipRole getRole() {
        return role;
    }

    public void setRole(MembershipRole role) {
        this.role = role;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    @Transient
    public UUID getUserId() {
        return user.getId();
    }

    @Transient
    public UUID getHouseholdId() {
        return household.getId();
    }
}
