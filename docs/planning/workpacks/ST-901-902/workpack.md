# Workpack: ST-901 + ST-902 — Points Ledger + Badges v0

## Sources of Truth
- Epic: `docs/planning/epics/EP-009/epic.md`
- Stories: `docs/planning/epics/EP-009/stories/ST-901-points-ledger.md`, `docs/planning/epics/EP-009/stories/ST-902-badges-achievements.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q2-gamification-motivation.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Draft** — SAFE S08 Scope (streak-free badges, idempotent points)

---

## Outcome
Backend gamification foundation:
1. Points ledger with task completion events
2. 5 milestone badges with auto-unlock
3. Progress endpoint for UI

---

## Key Decisions (SAFE S08 — Approved)
- Points model: 10 base + 5 on-time bonus
- No penalty for overdue (anti-shame)
- Reversible on task uncomplete (full rollback: base + bonus)
- **Idempotency:** Dedup by `(taskId, recipientUserId, reason)` — no double-award, handles reassignment
- **On-time edge case:** If `deadline = null`, no on-time bonus awarded
- 5 predefined badges (**streak-free**: ON_TIME_HERO instead of SEVEN_DAY_STREAK)
- No manual adjustment endpoint in S08

---

## Scope

### In Scope
- PointsLedger entity + repository
- PointsService (award, reverse)
- Badge entity + catalog (5 badges)
- BadgeService (check criteria, award)
- UserBadge entity
- `GET /households/{id}/gamification/progress` endpoint
- `GET /households/{id}/gamification/badges` endpoint
- Unit + integration tests
- DB migrations

### Out of Scope
- Streaks (ST-903)
- Privacy settings (ST-906)
- Web UI (ST-904)
- Manual adjustment endpoint (defer to admin story)

---

## Files to Create

### Entities
| Path | Purpose |
|------|---------|
| `services/backend/src/main/java/com/hometusk/gamification/domain/PointsLedger.java` | Points entries |
| `services/backend/src/main/java/com/hometusk/gamification/domain/PointsReason.java` | Enum |
| `services/backend/src/main/java/com/hometusk/gamification/domain/Badge.java` | Badge catalog |
| `services/backend/src/main/java/com/hometusk/gamification/domain/UserBadge.java` | Earned badges |

### Repositories
| Path | Purpose |
|------|---------|
| `services/backend/src/main/java/com/hometusk/gamification/repository/PointsLedgerRepository.java` | Points queries |
| `services/backend/src/main/java/com/hometusk/gamification/repository/BadgeRepository.java` | Badge catalog |
| `services/backend/src/main/java/com/hometusk/gamification/repository/UserBadgeRepository.java` | User badges |

### Services
| Path | Purpose |
|------|---------|
| `services/backend/src/main/java/com/hometusk/gamification/service/PointsService.java` | Award/reverse points |
| `services/backend/src/main/java/com/hometusk/gamification/service/BadgeService.java` | Badge criteria check |

### API
| Path | Purpose |
|------|---------|
| `services/backend/src/main/java/com/hometusk/gamification/api/GamificationController.java` | REST endpoints |
| `services/backend/src/main/java/com/hometusk/gamification/dto/GamificationProgressResponse.java` | Progress DTO |
| `services/backend/src/main/java/com/hometusk/gamification/dto/BadgeCatalogResponse.java` | Badges DTO |
| `services/backend/src/main/java/com/hometusk/gamification/dto/PointsEntryDto.java` | Entry DTO |
| `services/backend/src/main/java/com/hometusk/gamification/dto/BadgeDto.java` | Badge DTO |

### Tests
| Path | Purpose |
|------|---------|
| `services/backend/src/test/java/com/hometusk/gamification/service/PointsServiceTest.java` | Unit tests |
| `services/backend/src/test/java/com/hometusk/gamification/service/BadgeServiceTest.java` | Unit tests |
| `services/backend/src/test/java/com/hometusk/gamification/api/GamificationControllerIntegrationTest.java` | Integration tests |

### Migrations
| Path | Purpose |
|------|---------|
| `services/backend/src/main/resources/db/migration/V20__create_gamification_tables.sql` | DB schema |

---

## Implementation Plan

### Step 1: Create DB Migration
```sql
-- V20__create_gamification_tables.sql

CREATE TABLE badges (
    id UUID PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    criteria VARCHAR(500),
    icon_name VARCHAR(50)
);

CREATE TABLE points_ledger (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    household_id UUID NOT NULL REFERENCES households(id),
    task_id UUID REFERENCES tasks(id),
    points INTEGER NOT NULL,
    reason VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES users(id),
    note VARCHAR(500)
);

CREATE TABLE user_badges (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    household_id UUID NOT NULL REFERENCES households(id),
    badge_id UUID NOT NULL REFERENCES badges(id),
    earned_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, household_id, badge_id)
);

CREATE INDEX idx_points_user_household ON points_ledger(user_id, household_id);
CREATE INDEX idx_points_household_created ON points_ledger(household_id, created_at);
CREATE INDEX idx_user_badges_user ON user_badges(user_id, household_id);

-- Seed badges (S08 — streak-free)
INSERT INTO badges (id, code, name, description, criteria, icon_name) VALUES
    (gen_random_uuid(), 'FIRST_TASK', 'Task Starter', 'You completed your first task!', 'Complete 1 task', 'star'),
    (gen_random_uuid(), 'TEN_TASKS', 'Task Champion', 'You are on a roll!', 'Complete 10 tasks', 'trophy'),
    (gen_random_uuid(), 'WEEK_WARRIOR', 'Week Warrior', 'Productive week!', 'Complete 7+ tasks in one week', 'fire'),
    (gen_random_uuid(), 'ZONE_SPECIALIST', 'Zone Specialist', 'Master of your domain!', 'Complete 5+ tasks in one zone', 'target'),
    (gen_random_uuid(), 'ON_TIME_HERO', 'On-Time Hero', 'Beating the clock!', 'Complete 5 tasks before deadline', 'clock');
-- Note: SEVEN_DAY_STREAK deferred to S09 migration (requires ST-903)
```

### Step 2: Create Entities (JPA)

### Step 3: Create Repositories

### Step 4: Create PointsService (with idempotency)
- `awardPoints(userId, householdId, taskId, basePoints)` — on task complete
  - **Idempotency key:** `(taskId, userId, TASK_COMPLETED)`
  - If exists, return existing — no double-award
  - Handles reassignment: different userId = new award allowed
- `awardOnTimeBonus(userId, householdId, taskId)` — if before deadline
  - **Idempotency key:** `(taskId, userId, ON_TIME_BONUS)`
  - **Edge case:** If `deadline = null`, skip bonus entirely
- `reversePoints(userId, householdId, taskId)` — on task uncomplete
  - Creates entries with negative points for TASK_UNCOMPLETED and ON_TIME_BONUS_REVERSED
  - **Idempotency key:** `(taskId, userId, TASK_UNCOMPLETED)`
- `getTotalPoints(userId, householdId)` — sum query
- `getPointsThisWeek(userId, householdId)` — last 7 days
- `getHouseholdAggregate(householdId)` — sum all members (for S08 UI)

### Step 5: Create BadgeService
- `checkAndAwardBadges(userId, householdId)` — called after points
- Badge criteria checkers (strategy pattern)
- `getBadgeCatalog(householdId)` — with earned status

### Step 6: Create Controller
```java
@RestController
@RequestMapping("/api/v1/households/{householdId}/gamification")
public class GamificationController {

    @GetMapping("/progress")
    public GamificationProgressResponse getProgress(@PathVariable UUID householdId) {}

    @GetMapping("/badges")
    public BadgeCatalogResponse getBadges(@PathVariable UUID householdId) {}
}
```

### Step 7: Integrate with TaskService
- Event listener or direct call when task completed/uncompleted
- Consider `@TransactionalEventListener` for decoupling

### Step 8: Write Tests

---

## Verification Commands

```bash
cd /home/vad/Документы/hometusk/services/backend

# Build
./gradlew build

# Migration test
./gradlew flywayMigrate

# Unit tests
./gradlew test --tests "*PointsServiceTest*"
./gradlew test --tests "*BadgeServiceTest*"

# Integration tests
./gradlew test --tests "*GamificationController*"

# Spotless
./gradlew spotlessApply
```

---

## Contract-First Checklist
- [ ] Add `GET /households/{id}/gamification/progress` to OpenAPI
- [ ] Add `GET /households/{id}/gamification/badges` to OpenAPI
- [ ] Add schemas: GamificationProgressResponse, BadgeCatalogResponse, etc.

---

## Risks
| Risk | Mitigation |
|------|------------|
| Event ordering | Use transactional boundaries |
| Duplicate badge award | UNIQUE constraint + idempotent check |
| Performance on many tasks | Index on household_id, created_at |

---

## Rollback
- Delete gamification tables
- Remove controller
- Web shows "Progress unavailable"

---

## DoD Checklist
- [ ] Entities created and mapped
- [ ] Services implemented
- [ ] Controller returns correct data
- [ ] 403 for non-members
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Migration works
- [ ] OpenAPI updated
- [ ] Spotless applied
