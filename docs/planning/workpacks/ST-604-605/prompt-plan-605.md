# Codex PLAN Prompt: ST-605 — Notification Deduplication

## Mode
**PLAN ONLY** — Read-only exploration. NO file modifications allowed.

## Allowed Commands (Whitelist)
```
ls, find, cat, rg, grep, sed -n, head, tail, git status, git diff
```

## Forbidden
- Any file edits/writes/moves/deletes
- Network access
- Package install
- git commit/push
- Database operations

---

## Task
Plan the implementation of notification deduplication via idempotency key.

## Sources of Truth (MUST READ)
1. `docs/planning/epics/EP-007/stories/ST-605-notification-dedup.md` — Story spec with ACs
2. `services/backend/src/main/java/com/hometusk/notifications/service/NotificationService.java` — Current notification creation
3. `services/backend/src/main/java/com/hometusk/notifications/domain/Notification.java` — Entity
4. `services/backend/src/main/java/com/hometusk/notifications/repository/NotificationRepository.java` — Repository
5. `services/backend/src/main/resources/db/migration/` — Existing migrations pattern

## Architecture Context
- NotificationService has `createNotification()` method
- Notification entity already exists with standard fields
- Need to add `idempotencyKey` field and dedup check
- 5-minute time window for deduplication

---

## Critical Constraints (MUST FOLLOW)

### 1. Idempotency Key Format
```
{type}:{entityId}:{userId}:{5minWindowStart}
```
Example: `task_assigned:550e8400-e29b-41d4-a716-446655440000:user123:1706025600000`

### 2. Time Window Calculation
```java
// Round timestamp to 5-minute window (300000 ms)
long windowStart = (timestamp.toEpochMilli() / 300000) * 300000;
```

### 3. Dedup Logic
- Generate key BEFORE saving
- Check `existsByIdempotencyKey(key)`
- If exists → log.debug("Duplicate suppressed") and return early
- If not exists → save with idempotencyKey

### 4. No Rate Limiting
- Story explicitly excludes rate limiting
- Only dedup for exact same event within time window

---

## Exploration Tasks

### Task 1: Understand NotificationService Structure
- Read `NotificationService.java` — find `createNotification()` method
- What parameters does it receive?
- Where is the save call?
- Is there already any dedup logic?

### Task 2: Understand Notification Entity
- Read `Notification.java` — current fields
- Constructor signature
- Any existing constraints?

### Task 3: Understand Repository
- Read `NotificationRepository.java` — existing methods
- Spring Data JPA pattern used?

### Task 4: Check Migration Patterns
- List existing migrations in `db/migration/`
- What naming convention?
- What's the next version number?

### Task 5: Check Existing Tests
- Are there NotificationService tests?
- What patterns do they use?

---

## Output Format

After exploration, provide:

1. **Verification of Assumptions**
   - Confirm NotificationService structure
   - Confirm Notification entity fields
   - Confirm repository pattern
   - Confirm migration naming

2. **Files to Create**
   - Migration file path and name
   - Test file path

3. **Files to Modify** (with specific changes)
   - `Notification.java` — add idempotencyKey field
   - `NotificationRepository.java` — add existsByIdempotencyKey
   - `NotificationService.java` — add dedup check

4. **Key Implementation Details**
   - Where exactly to add dedup check
   - How to handle entityId extraction from payload
   - Constructor changes needed

5. **Risks/Blockers** (if any)

6. **Questions** (if clarification needed)

---

## Stop Conditions
- If NotificationService structure differs significantly → STOP and describe
- If existing migrations use different pattern → STOP and describe
- Do NOT guess without evidence from codebase
