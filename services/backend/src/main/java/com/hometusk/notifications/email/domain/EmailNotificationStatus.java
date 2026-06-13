package com.hometusk.notifications.email.domain;

public enum EmailNotificationStatus {
    PENDING,
    SENT,
    FAILED,
    RETRY_SCHEDULED,
    CANCELLED;

    public boolean isDeliverable() {
        return this == PENDING || this == RETRY_SCHEDULED;
    }
}
