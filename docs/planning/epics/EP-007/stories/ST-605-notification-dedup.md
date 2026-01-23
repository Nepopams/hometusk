# Story: ST-605 — Notification Deduplication

## Sources of Truth
- Epic: `docs/planning/epics/EP-007/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q2-notifications-realtime.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Ready** — DoR complete, pending sprint commitment

## Priority
P3 (Nice-to-have)

## Points
2

---

## Description
Prevent duplicate notifications from being created when the same event occurs multiple times (e.g., rapid task reassignments) or when batch operations happen.

### User Value
As a household member, I don't want to be spammed with duplicate or near-duplicate notifications, so my notification list remains useful and not overwhelming.

### Technical Approach
- Idempotency key based on event signature (type + entityId + timestamp window)
- Check for existing notification before creating
- Rate limiting: max N notifications of same type per minute per user
- Optional: batch similar notifications ("3 tasks assigned to you")

---

## Acceptance Criteria

### AC-1: No Duplicate for Same Event
```gherkin
Given task T1 is assigned to user U1
When same assignment event fires twice (due to retry/bug)
Then only one notification created for U1
```

### AC-2: Rate Limiting
```gherkin
Given user receives 10 task_assigned notifications in 1 minute
Then only first 5 are created (configurable limit)
And subsequent ones are suppressed
```

### AC-3: Idempotency Key
```gherkin
Given notification created with idempotency key "task_assigned:task123:2026-01-23T10:00"
When same key used again
Then existing notification returned
And no duplicate created
```

### AC-4: Different Events Not Affected
```gherkin
Given task T1 assigned → notification created
When task T1 completed → separate notification
Then both notifications exist (different event types)
```

### AC-5: Time Window for Dedup
```gherkin
Given task T1 assigned to U1 at 10:00
When task T1 assigned to U1 again at 10:30 (different assignment)
Then second notification IS created (outside 5-min window)
```

---

## Test Strategy

### Unit Tests
- Test idempotency key generation
- Test duplicate detection
- Test rate limiting logic
- Test time window logic

### Integration Tests
- Create same notification twice → verify single entry
- Create rapid notifications → verify rate limit
- Create notifications outside window → verify both created

---

## Flags

| Flag | Value | Notes |
|------|-------|-------|
| contract_impact | no | Internal optimization |
| adr_needed | no | Standard dedup pattern |
| diagrams_needed | no | |
| security_sensitive | no | |
| traceability_critical | no | |

---

## Dependencies
- NotificationService

## Blocked By
- None

---

## Implementation Notes

### Idempotency Key Generation
```java
private String generateIdempotencyKey(
    NotificationType type,
    UUID entityId,
    UUID userId,
    Instant timestamp
) {
    // Round timestamp to 5-minute window
    long windowStart = (timestamp.toEpochMilli() / 300000) * 300000;
    return String.format("%s:%s:%s:%d", type, entityId, userId, windowStart);
}
```

### Dedup Check in createNotification
```java
private void createNotification(
        Household household,
        User recipient,
        NotificationType type,
        NotificationPayloadDto payload,
        UUID correlationId) {

    // Generate idempotency key
    String idempotencyKey = generateIdempotencyKey(
        type,
        payload.entityId(),
        recipient.getId(),
        Instant.now()
    );

    // Check for existing
    if (notificationRepository.existsByIdempotencyKey(idempotencyKey)) {
        log.debug("Duplicate notification suppressed: {}", idempotencyKey);
        return;
    }

    // Rate limit check
    long recentCount = notificationRepository.countByUserAndTypeAndCreatedAtAfter(
        recipient.getId(),
        type,
        Instant.now().minusSeconds(60)
    );
    if (recentCount >= MAX_NOTIFICATIONS_PER_MINUTE) {
        log.debug("Rate limit reached for user {} type {}", recipient.getId(), type);
        return;
    }

    String payloadJson = toJson(payload);
    Notification notification = new Notification(
        household, recipient, type, payloadJson, correlationId, idempotencyKey
    );
    notificationRepository.save(notification);
}
```

### Repository Addition
```java
boolean existsByIdempotencyKey(String idempotencyKey);

long countByUser_IdAndTypeAndCreatedAtAfter(UUID userId, NotificationType type, Instant since);
```

### Entity Addition
```java
@Column(name = "idempotency_key", unique = true)
private String idempotencyKey;
```

### Migration
```sql
ALTER TABLE notifications ADD COLUMN idempotency_key VARCHAR(255);
CREATE UNIQUE INDEX idx_notifications_idempotency_key ON notifications(idempotency_key);
```

### Configuration
```yaml
hometusk:
  notifications:
    dedup-window-minutes: 5
    max-per-minute-per-type: 5
```

---

## Future Considerations (Out of Scope)
- Batch notifications ("3 tasks assigned to you") - requires more complex logic
- User-configurable notification preferences
- Quiet hours / Do not disturb
