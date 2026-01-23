# Workpack: ST-604 + ST-605 — Degraded Fallback + Notification Deduplication

## Sources of Truth
- Epic: `docs/planning/epics/EP-007/epic.md`
- Stories:
  - `docs/planning/epics/EP-007/stories/ST-604-degraded-fallback.md`
  - `docs/planning/epics/EP-007/stories/ST-605-notification-dedup.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q2-notifications-realtime.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Ready** — Stretch scope for S06, may defer to S07

---

## Outcome
- ST-604: Web client falls back to polling when SSE unavailable
- ST-605: Backend prevents duplicate notifications

---

## Acceptance Criteria Summary

### ST-604 (Degraded Fallback)
1. Fallback to polling after max SSE retries (5 attempts)
2. Polling fetches new notifications every 30 seconds
3. Subtle degraded mode indicator
4. Auto-recovery to SSE when available
5. No duplicate notifications in UI
6. Silent degradation (no error modals)

### ST-605 (Deduplication)
1. No duplicate notification for same event
2. Rate limiting: max 5 per minute per type per user
3. Idempotency key based on type + entityId + time window
4. Different event types still create separate notifications
5. Outside time window (5 min) creates new notification

---

## Files to Change/Create

### ST-604 (Web)
| Path | Changes |
|------|---------|
| `clients/web/src/hooks/useNotificationStream.ts` | Add polling fallback logic |
| `clients/web/src/components/notifications/ConnectionStatus.tsx` | NEW: Degraded indicator |
| `clients/web/src/styles/index.css` | Degraded indicator styles |

### ST-605 (Backend)
| Path | Changes |
|------|---------|
| `services/backend/src/main/java/com/hometusk/notifications/domain/Notification.java` | Add `idempotencyKey` field |
| `services/backend/src/main/java/com/hometusk/notifications/repository/NotificationRepository.java` | Add `existsByIdempotencyKey()`, `countByUser_IdAndTypeAndCreatedAtAfter()` |
| `services/backend/src/main/java/com/hometusk/notifications/service/NotificationService.java` | Add dedup logic in `createNotification()` |
| `services/backend/src/main/resources/db/migration/V{N}__add_notification_idempotency.sql` | NEW: Add idempotency_key column |
| `services/backend/src/test/java/com/hometusk/notifications/service/NotificationDeduplicationTest.java` | NEW: Dedup tests |

---

## Implementation Plan

### ST-604: Degraded Fallback (Web)

#### Step 1: Update useNotificationStream Hook
- Add `mode` state: `'sse' | 'polling'`
- Track retry count, switch to polling after 5 failures
- Start polling interval (30s)
- Periodically attempt SSE reconnect (60s)

#### Step 2: Add Polling Logic
- `fetchNewNotifications()` with `since` parameter
- Deduplicate in `addNotification` (check by id)
- Update `lastFetch` timestamp

#### Step 3: Create ConnectionStatus Component
- Show subtle icon when in polling mode
- Tooltip: "Updates may be delayed"

### ST-605: Deduplication (Backend)

#### Step 1: Database Migration
- Add `idempotency_key` column (VARCHAR 255, nullable, unique)
- Create index

#### Step 2: Update Notification Entity
- Add `idempotencyKey` field
- Update constructor

#### Step 3: Update Repository
- Add `existsByIdempotencyKey(String key)`
- Add `countByUser_IdAndTypeAndCreatedAtAfter(UUID userId, NotificationType type, Instant since)`

#### Step 4: Update NotificationService
- Generate idempotency key: `{type}:{entityId}:{userId}:{5minWindow}`
- Check existence before save
- Check rate limit before save
- Log suppressed notifications

#### Step 5: Write Tests
- Test duplicate suppression
- Test rate limiting
- Test time window logic

---

## Verification Commands

### ST-604 (Web)
```bash
cd /home/vad/Документы/hometusk/clients/web
npm run build
npm run lint
```

### ST-605 (Backend)
```bash
cd /home/vad/Документы/hometusk/services/backend
./gradlew build
./gradlew test --tests "*NotificationDeduplication*"
./gradlew spotlessCheck
```

---

## Tests

### ST-604 Manual Tests
1. Block SSE in DevTools → verify polling starts
2. Verify notifications arrive via polling (30s interval)
3. Unblock SSE → verify auto-recovery
4. Check for duplicate notifications in UI

### ST-605 Unit Tests
- `generateIdempotencyKey_shouldCreateConsistentKey`
- `createNotification_duplicateKey_shouldSkip`
- `createNotification_rateLimit_shouldSkip`
- `createNotification_outsideWindow_shouldCreate`

### ST-605 Integration Tests
- `notifyTaskAssigned_duplicate_shouldNotCreateSecond`
- `notifyTaskAssigned_rapidFire_shouldRateLimit`

---

## DoD Checklist

### ST-604
- [ ] Polling fallback works
- [ ] Degraded indicator shown
- [ ] Auto-recovery works
- [ ] No UI duplicates
- [ ] npm run build passes

### ST-605
- [ ] Migration runs successfully
- [ ] Duplicate prevention works
- [ ] Rate limiting works
- [ ] Tests pass
- [ ] ./gradlew build passes

---

## Risks
| Risk | Mitigation |
|------|------------|
| Polling causes server load | 30s interval is reasonable |
| Rate limit too aggressive | Make configurable |
| Idempotency key collision | Include userId to avoid |

---

## Rollback
- ST-604: Remove polling logic, SSE only (users may miss notifications)
- ST-605: Remove idempotency check, accept possible duplicates

---

## Prompt Pack
- `prompt-plan.md`: Explore existing infrastructure
- `prompt-apply.md`: Implementation after plan approval
- `prompt-review.md`: Review after apply completion
