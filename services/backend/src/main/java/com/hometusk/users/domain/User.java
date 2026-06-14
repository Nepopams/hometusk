package com.hometusk.users.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "external_id", nullable = false, unique = true)
    private String externalId;

    @Column(name = "email")
    private String email;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Convert(converter = EmailSourceConverter.class)
    @Column(name = "email_source", nullable = false)
    private EmailSource emailSource = EmailSource.UNKNOWN;

    @Column(name = "email_updated_at")
    private Instant emailUpdatedAt;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User() {}

    public User(String externalId, String email, String displayName) {
        Instant now = Instant.now();
        this.externalId = externalId;
        this.email = normalizeEmail(email);
        this.emailVerified = false;
        this.emailSource = EmailSource.UNKNOWN;
        this.emailUpdatedAt = this.email == null ? null : now;
        this.displayName = displayName;
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.emailSource == null) {
            this.emailSource = EmailSource.UNKNOWN;
        }
        if (this.email != null) {
            this.email = normalizeEmail(this.email);
            if (this.emailUpdatedAt == null) {
                this.emailUpdatedAt = now;
            }
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (!Objects.equals(this.email, normalizedEmail)) {
            this.email = normalizedEmail;
            this.emailUpdatedAt = normalizedEmail == null ? null : Instant.now();
        }
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public EmailSource getEmailSource() {
        return emailSource;
    }

    public void setEmailSource(EmailSource emailSource) {
        this.emailSource = emailSource == null ? EmailSource.UNKNOWN : emailSource;
    }

    public Instant getEmailUpdatedAt() {
        return emailUpdatedAt;
    }

    public boolean isEmailNotificationEligible() {
        return email != null && emailVerified;
    }

    public boolean syncEmailFromIdentityProvider(String claimedEmail, Boolean claimedEmailVerified) {
        String normalizedEmail = normalizeEmail(claimedEmail);
        if (normalizedEmail == null) {
            return false;
        }

        boolean updated = false;

        if (!Objects.equals(this.email, normalizedEmail)) {
            this.email = normalizedEmail;
            this.emailUpdatedAt = Instant.now();
            this.emailSource = EmailSource.IDP_CLAIM;
            this.emailVerified = Boolean.TRUE.equals(claimedEmailVerified);
            return true;
        }

        if (this.emailSource != EmailSource.IDP_CLAIM) {
            this.emailSource = EmailSource.IDP_CLAIM;
            updated = true;
        }

        if (claimedEmailVerified != null && this.emailVerified != claimedEmailVerified) {
            this.emailVerified = claimedEmailVerified;
            updated = true;
        }

        return updated;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private static String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }

        String trimmed = email.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        return trimmed.toLowerCase(Locale.ROOT);
    }
}
