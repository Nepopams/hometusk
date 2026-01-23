# Story: ST-605 — Notification Deduplication

## Sources of Truth
- Epic: `docs/planning/epics/EP-007/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q2-notifications-realtime.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Ready** — DoR complete, pending sprint commitment (S07 candidate)

## Priority
P3 (Nice-to-have)

## Points
2

---

## Description
Prevent duplicate notifications from being created when the same event occurs multiple times (e.g., rapid task reassignments, retries, or race conditions).

### User Value
As a household member, I don't want to see duplicate notifications for the same event, so my notification list remains clean and useful.

### Technical Approach
- Idempotency key based on event signature (type + entityId + userId + time window)
- Check for existing notification before creating
- Time window based deduplication (5 min) — same event within window = same notification

---

## Scope Clarification

### In Scope (This Story)
- **Deduplication via idempotency key** — prevent exact duplicate notifications for same event

### Out of Scope (Deferred)
- **Rate limiting** — suppressing notifications after N per minute
  - Reason: Risk of losing important notifications; requires careful design and user feedback
  - Recommend: Separate story with explicit product decision on limits and UI feedback
- **Batch notifications** — grouping ("3 tasks assigned to you")
  - Reason: Requires more complex logic and UI changes
  - Recommend: Future initiative

---

## Acceptance Criteria

### AC-1: No Duplicate for Same Event
```gherkin
Given task T1 is assigned to user U1
When same assignment event fires twice (due to retry/bug)
Then only one notification created for U1
```

### AC-2: Idempotency Key Format
```gherkin
Given notification creation request
Then idempotency key is generated as: "{type}:{entityId}:{userId}:{5minWindow}"
And key is stored with notification
```

### AC-3: Duplicate Detected by Key
```gherkin
Given notification with idempotency key "task_assigned:task123:user456:12345"
When another notification with same key is created
Then existing notification is returned (no new record)
And log entry indicates duplicate suppressed
```

### AC-4: Different Events Not Affected
```gherkin
Given task T1 assigned → notification created
When task T1 completed → separate notification
Then both notifications exist (different event types = different keys)
```

### AC-5: Time Window for Dedup
```gherkin
Given task T1 assigned to U1 at 10:00
And notification created with key "task_assigned:task123:user456:12345"
When task T1 assigned to U1 again at 10:30 (different 5-min window)
Then second notification IS created (different time window = different key)
```

---

## Test Strategy

### Unit Tests
- `generateIdempotencyKey_shouldIncludeAllComponents`
- `generateIdempotencyKey_sameInputsSameWindow_shouldReturnSameKey`
- `generateIdempotencyKey_differentWindow_shouldReturnDifferentKey`
- `createNotification_duplicateKey_shouldSkip`
- `createNotification_differentKey_shouldCreate`

### Integration Tests
- `notifyTaskAssigned_duplicate_shouldNotCreateSecond`
- `notifyTaskAssigned_differentWindow_shouldCreateBoth`

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
- None (can be implemented independently)

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
    // Round timestamp to 5-minute window (300000 ms)
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

    // Check for existing (dedup)
    if (notificationRepository.existsByIdempotencyKey(idempotencyKey)) {
        log.debug("Duplicate notification suppressed: {}", idempotencyKey);
        return;
    }

    // Create notification with key
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
```

### Entity Addition
```java
@Column(name = "idempotency_key", unique = true)
private String idempotencyKey;

public Notification(..., String idempotencyKey) {
    // ... existing fields ...
    this.idempotencyKey = idempotencyKey;
}
```

### Migration
```sql
-- V{N}__add_notification_idempotency_key.sql
ALTER TABLE notifications ADD COLUMN idempotency_key VARCHAR(255);
CREATE UNIQUE INDEX idx_notifications_idempotency_key ON notifications(idempotency_key);
```

### Configuration (Optional)
```yaml
hometusk:
  notifications:
    dedup-window-minutes: 5  # Time window for deduplication
```

---

## Future Considerations (Explicitly Out of Scope)

### Rate Limiting
- Suppressing notifications after N per minute per type per user
- Requires product decision: what happens to suppressed notifications?
- Options: silent drop, batch into one, show count badge
- Recommend: Separate story with explicit UX design

### Batch Notifications
- Grouping similar notifications ("3 tasks assigned to you")
- Requires significant UI changes
- Recommend: Future initiative after validating notification usage patterns
