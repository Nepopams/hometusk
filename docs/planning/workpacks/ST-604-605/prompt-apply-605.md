# Codex APPLY Prompt: ST-605 — Notification Deduplication

## Mode
**APPLY** — Implementation mode. File modifications allowed.

## Allowed Operations
```
- Create/edit Java files in services/backend/src/
- Create migration files in services/backend/src/main/resources/db/migration/
- Run gradle build/test commands
```

## Forbidden
- Modifying web client files
- Changing unrelated functionality
- Adding rate limiting (explicitly out of scope)

---

## Task
Implement notification deduplication via idempotency key with 5-minute time window.

## Sources of Truth
1. `docs/planning/epics/EP-007/stories/ST-605-notification-dedup.md` — Story spec
2. `services/backend/src/main/java/com/hometusk/notifications/service/NotificationService.java` — Service to modify
3. `services/backend/src/main/java/com/hometusk/notifications/domain/Notification.java` — Entity to modify

## Approved Plan Summary
- Add `idempotencyKey` field to Notification entity
- Add `existsByIdempotencyKey()` and `findByIdempotencyKey()` to repository
- Generate key in NotificationService before save
- Return existing notification if duplicate (per AC-3)
- Handle race condition via unique index + exception catch

---

## Implementation

### Step 1: Create Migration
```sql
-- services/backend/src/main/resources/db/migration/V017__add_notification_idempotency_key.sql
ALTER TABLE notifications ADD COLUMN idempotency_key VARCHAR(255);
CREATE UNIQUE INDEX idx_notifications_idempotency_key ON notifications(idempotency_key);
```

### Step 2: Update Notification Entity
```java
// Add to Notification.java

@Column(name = "idempotency_key")
private String idempotencyKey;

// Update constructor to accept idempotencyKey
public Notification(
    Household household,
    User recipient,
    NotificationType type,
    String payloadJson,
    UUID correlationId,
    String idempotencyKey  // NEW
) {
    // ... existing code ...
    this.idempotencyKey = idempotencyKey;
}

// Add getter
public String getIdempotencyKey() {
    return idempotencyKey;
}
```

### Step 3: Update NotificationRepository
```java
// Add to NotificationRepository.java

boolean existsByIdempotencyKey(String idempotencyKey);

Optional<Notification> findByIdempotencyKey(String idempotencyKey);
```

### Step 4: Update NotificationService
```java
// Add method for key generation
private String generateIdempotencyKey(
    NotificationType type,
    UUID entityId,
    UUID userId,
    Instant timestamp
) {
    // Round timestamp to 5-minute window (300000 ms)
    long windowStart = (timestamp.toEpochMilli() / 300000) * 300000;
    return String.format("%s:%s:%s:%d",
        type.name().toLowerCase(),
        entityId,
        userId,
        windowStart
    );
}

// Update createNotification method
private NotificationDto createNotification(
    Household household,
    User recipient,
    NotificationType type,
    NotificationPayloadDto payload,
    UUID correlationId
) {
    Instant now = Instant.now();

    // Generate idempotency key
    String idempotencyKey = generateIdempotencyKey(
        type,
        payload.entityId(),
        recipient.getId(),
        now
    );

    // Check for existing (dedup)
    Optional<Notification> existing = notificationRepository.findByIdempotencyKey(idempotencyKey);
    if (existing.isPresent()) {
        log.debug("Duplicate notification suppressed: {}", idempotencyKey);
        return toDto(existing.get());
    }

    // Create new notification
    String payloadJson = toJson(payload);
    Notification notification = new Notification(
        household,
        recipient,
        type,
        payloadJson,
        correlationId,
        idempotencyKey
    );

    try {
        notification = notificationRepository.save(notification);
    } catch (DataIntegrityViolationException e) {
        // Race condition: another thread saved first
        log.debug("Duplicate notification (race): {}", idempotencyKey);
        return notificationRepository.findByIdempotencyKey(idempotencyKey)
            .map(this::toDto)
            .orElseThrow(() -> e);
    }

    // Publish event only for new notifications
    eventPublisher.publishEvent(new NotificationCreatedEvent(toDto(notification)));

    return toDto(notification);
}
```

### Step 5: Write Tests

#### Unit Test: NotificationServiceTest.java
```java
@Test
void generateIdempotencyKey_shouldIncludeAllComponents() {
    // Test key format
}

@Test
void generateIdempotencyKey_sameInputsSameWindow_shouldReturnSameKey() {
    // Test same window returns same key
}

@Test
void generateIdempotencyKey_differentWindow_shouldReturnDifferentKey() {
    // Test different 5-min windows return different keys
}

@Test
void createNotification_duplicateKey_shouldReturnExisting() {
    // Mock repository to return existing
    // Verify no new save, no event published
}

@Test
void createNotification_newKey_shouldSaveAndPublish() {
    // Verify save called, event published
}
```

#### Integration Test: NotificationDeduplicationIntegrationTest.java
```java
@Test
void notifyTaskAssigned_duplicate_shouldNotCreateSecond() {
    // Create first notification
    // Call again with same parameters
    // Verify only one notification in DB
}

@Test
void notifyTaskAssigned_differentWindow_shouldCreateBoth() {
    // Create first notification
    // Manipulate time or use different window
    // Verify two notifications
}
```

---

## Verification Commands

```bash
cd /home/vad/Документы/hometusk/services/backend

# Build
./gradlew build

# Run specific tests
./gradlew test --tests "*NotificationService*"
./gradlew test --tests "*NotificationDeduplication*"

# Format check
./gradlew spotlessCheck
```

---

## DoD Checklist
- [ ] Migration runs successfully
- [ ] Code follows project conventions
- [ ] Spotless formatting applied
- [ ] Unit tests written and passing
- [ ] Integration tests written and passing
- [ ] Duplicate notifications prevented (AC-1)
- [ ] Existing notification returned on duplicate (AC-3)
- [ ] Different event types create separate notifications (AC-4)
- [ ] Different time windows create separate notifications (AC-5)
- [ ] No rate limiting added (explicit non-goal)

---

## STOP-THE-LINE Rules
- If Notification constructor signature change breaks other code → STOP
- If migration fails on existing data → STOP
- Do NOT add rate limiting (out of scope)
