package com.hometusk.mobile.domain;

import com.hometusk.users.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mobile_devices")
public class MobileDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 20)
    private MobilePlatform platform;

    @Enumerated(EnumType.STRING)
    @Column(name = "push_provider", nullable = false, length = 20)
    private PushProvider pushProvider;

    @Column(name = "push_token", nullable = false, length = 512)
    private String pushToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MobileDeviceStatus status = MobileDeviceStatus.ACTIVE;

    @Column(name = "device_name", length = 120)
    private String deviceName;

    @Column(name = "app_version", length = 40)
    private String appVersion;

    @Column(name = "locale", length = 35)
    private String locale;

    @Column(name = "timezone", length = 80)
    private String timezone;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MobileDevice() {}

    public MobileDevice(User user, MobilePlatform platform, PushProvider pushProvider, String pushToken) {
        this.user = user;
        this.platform = platform;
        this.pushProvider = pushProvider;
        this.pushToken = pushToken;
        this.status = MobileDeviceStatus.ACTIVE;
        this.lastSeenAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (lastSeenAt == null) {
            lastSeenAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public void refresh(MobilePlatform platform, String deviceName, String appVersion, String locale, String timezone) {
        this.platform = platform;
        this.status = MobileDeviceStatus.ACTIVE;
        this.deviceName = deviceName;
        this.appVersion = appVersion;
        this.locale = locale;
        this.timezone = timezone;
        this.lastSeenAt = Instant.now();
    }

    public void updateToken(String pushToken) {
        this.pushToken = pushToken;
        this.lastSeenAt = Instant.now();
    }

    public void updateStatus(MobileDeviceStatus status) {
        this.status = status;
        this.lastSeenAt = Instant.now();
    }

    public void updateMetadata(String appVersion, String locale, String timezone) {
        if (appVersion != null) {
            this.appVersion = appVersion;
        }
        if (locale != null) {
            this.locale = locale;
        }
        if (timezone != null) {
            this.timezone = timezone;
        }
        this.lastSeenAt = Instant.now();
    }

    public void deactivate() {
        this.status = MobileDeviceStatus.INACTIVE;
        this.lastSeenAt = Instant.now();
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

    public MobilePlatform getPlatform() {
        return platform;
    }

    public PushProvider getPushProvider() {
        return pushProvider;
    }

    public String getPushToken() {
        return pushToken;
    }

    public MobileDeviceStatus getStatus() {
        return status;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public String getLocale() {
        return locale;
    }

    public String getTimezone() {
        return timezone;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
