# Story: ST-605 — Notification Deduplication

## Sources of Truth
- Epic: `docs/planning/epics/EP-007/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q2-notifications-realtime.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Done** — Implementation complete (commit 1aabdda)

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
- Idempotency key based on event signature (type + entityId + userId + correlationId or time window)
- **Unique index on idempotency_key** (DB-enforced uniqueness)
- Check-then-insert with **race-safe fallback** (catch unique constraint violation)
- Two-tier key strategy: correlationId-first, time window as fallback

---

## Scope Clarification

### In Scope (This Story)
- **Deduplication via idempotency key** — prevent exact duplicate notifications for same event
- **Race-safe insert pattern** — handle concurrent inserts gracefully
- **CorrelationId-first key design** — stable dedup across command retries

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

### AC-2: Idempotency Key Format (Two-Tier Rule)
```gherkin
Given notification creation request with correlationId present
Then idempotency key is generated as: "{type}:{entityId}:{userId}:{correlationId}"

Given notification creation request without correlationId (or null)
Then idempotency key is generated as: "{type}:{entityId}:{userId}:{5minWindow}"
```

**Priority:** CorrelationId-based key is preferred. Time window is fallback only.

### AC-3: Duplicate Detected by Key
```gherkin
Given notification with idempotency key "task_assigned:task123:user456:corr789"
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

### AC-5: Time Window Fallback (Only When No CorrelationId)
```gherkin
Given notification without correlationId at 10:00
And notification created with key "task_assigned:task123:user456:12345" (5-min window)
When same event without correlationId fires at 10:03 (same window)
Then duplicate suppressed (same key)

When same event without correlationId fires at 10:30 (different window)
Then second notification IS created (different key)
```

**Note:** When correlationId is present, time window is NOT used. The correlationId provides stable dedup regardless of timing.

### AC-6: Race Condition Handled Gracefully
```gherkin
Given two concurrent requests to create notification with same idempotency key
When both pass the "exists" check simultaneously
And both attempt to insert
Then one succeeds (creates notification)
And other catches DataIntegrityViolationException
And other returns existing notification without leaking an error
And retrieval uses REQUIRES_NEW transaction (clean persistence context)
```

---

## Why CorrelationId-First

The correlationId (or stable eventId) provides **deterministic dedup** that doesn't depend on timing:

| Scenario | Time-Window Only | CorrelationId-First |
|----------|------------------|---------------------|
| Retry at 10:01, 10:02 (same window) | ✅ Deduped | ✅ Deduped |
| Retry at 10:01, 10:07 (different window) | ❌ Duplicate created | ✅ Deduped (same correlationId) |
| Legit re-assign at 10:01, 10:03 (same window) | ❌ Suppressed incorrectly | ✅ Both created (different correlationIds) |
| Legit re-assign at 10:01, 10:07 (different window) | ✅ Both created | ✅ Both created |

**Key insight:** Time-window dedup can suppress legitimate notifications (user unassigns then re-assigns within 5 minutes). CorrelationId ensures each command produces exactly one notification, regardless of timing.

---

## Test Strategy

### Unit Tests
- `generateIdempotencyKey_withCorrelationId_shouldUseCorrelationId`
- `generateIdempotencyKey_withoutCorrelationId_shouldUseFiveMinWindow`
- `generateIdempotencyKey_sameCorrelationId_shouldReturnSameKey`
- `generateIdempotencyKey_differentCorrelationId_shouldReturnDifferentKey`
- `generateIdempotencyKey_sameWindowNoCorrelationId_shouldReturnSameKey`
- `generateIdempotencyKey_differentWindowNoCorrelationId_shouldReturnDifferentKey`
- `createNotification_duplicateKey_shouldReturnExisting`
- `createNotification_differentKey_shouldCreate`

### Integration Tests
- `notifyTaskAssigned_sameCorrelationId_shouldNotCreateSecond`
- `notifyTaskAssigned_differentCorrelationId_shouldCreateBoth`
- `notifyTaskAssigned_noCorrelationId_sameWindow_shouldNotCreateSecond`
- `notifyTaskAssigned_noCorrelationId_differentWindow_shouldCreateBoth`
- `notifyTaskAssigned_racyDuplicate_shouldHandleGracefully`

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

### Idempotency Key Generation (Two-Tier)
```java
private String generateIdempotencyKey(
    NotificationType type,
    UUID entityId,
    UUID userId,
    UUID correlationId,
    Instant timestamp
) {
    String baseKey = String.format("%s:%s:%s", type.name().toLowerCase(), entityId, userId);

    if (correlationId != null) {
        // Preferred: stable key based on command correlation
        return baseKey + ":" + correlationId;
    } else {
        // Fallback: time-window based (for legacy callers without correlationId)
        long windowStart = (timestamp.toEpochMilli() / 300000) * 300000;
        return baseKey + ":" + windowStart;
    }
}
```

### Race-Safe Dedup Pattern (Actual Implementation)

**Pattern:** Check first → insert → catch unique constraint violation → return existing (via REQUIRES_NEW)

```java
private NotificationDto createNotification(
        Household household,
        User recipient,
        NotificationType type,
        NotificationPayloadDto payload,
        UUID correlationId) {

    Instant now = Instant.now();
    String idempotencyKey = generateIdempotencyKey(
        type,
        payload != null ? payload.entityId() : null,
        recipient.getId(),
        correlationId,
        now
    );

    // Step 1: Optimistic check (fast path for duplicates)
    Optional<Notification> existing = notificationRepository.findByIdempotencyKey(idempotencyKey);
    if (existing.isPresent()) {
        log.debug("Duplicate notification suppressed: {}", idempotencyKey);
        return toDto(existing.get());
    }

    // Step 2: Attempt insert
    String payloadJson = toJson(payload);
    Notification notification = new Notification(
        household, recipient, type, payloadJson, correlationId, idempotencyKey
    );

    try {
        notification = notificationRepository.save(notification);
    } catch (DataIntegrityViolationException e) {
        // Step 3: Race condition — another thread inserted first
        // Use REQUIRES_NEW to get clean persistence context
        log.debug("Duplicate notification (race): {}", idempotencyKey);
        return fetchExistingInNewTransaction(idempotencyKey, e);
    }

    // Step 4: Publish event for SSE delivery
    NotificationDto dto = toDto(notification);
    eventPublisher.publishEvent(new NotificationCreatedEvent(dto));
    return dto;
}
```

### Post-Exception Retrieval Safety (REQUIRES_NEW)

After a `DataIntegrityViolationException`, the current JPA persistence context may be in an inconsistent state. We use a new transaction for safe retrieval:

```java
@Service
public class NotificationLookupService {

    private final NotificationRepository notificationRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<Notification> findByIdempotencyKey(String idempotencyKey) {
        return notificationRepository.findByIdempotencyKey(idempotencyKey);
    }
}

// In NotificationService
private NotificationDto fetchExistingInNewTransaction(String idempotencyKey, Exception cause) {
    return notificationLookupService.findByIdempotencyKey(idempotencyKey)
            .map(this::toDto)
            .orElseThrow(() -> new IllegalStateException(
                "Race dedup failed: notification not found after constraint violation", cause));
}
```

**Why REQUIRES_NEW:**
- JPA/Hibernate marks the session as "rollback-only" after certain exceptions
- Cached entities may be stale or in an undefined state
- Query results may come from the L1 cache instead of the database
- New transaction ensures clean persistence context and fresh DB read

### Repository Addition
```java
Optional<Notification> findByIdempotencyKey(String idempotencyKey);
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
    dedup-window-minutes: 5  # Fallback time window (only when no correlationId)
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
