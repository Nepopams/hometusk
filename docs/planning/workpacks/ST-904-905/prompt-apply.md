# Codex APPLY Prompt: ST-904 + ST-905 — Gamification UI + Security

## Mode
**APPLY** — implement the approved plan.

## Sources of Truth
- Workpack: `docs/planning/workpacks/ST-904-905/workpack.md`
- Stories:
  - `docs/planning/epics/EP-009/stories/ST-904-gamification-ui.md`
  - `docs/planning/epics/EP-009/stories/ST-905-security-boundaries.md`
- DoD: `docs/_governance/dod.md`

---

## Clarifications (from PLAN review)
- **Frontend path**: `clients/web` (NOT `services/web`)
- **Household totals**: all-time aggregates (NOT "this week") — matches current backend API
- **Copy**: "Total tasks completed", "Total points" (no weekly qualifier)

---

## Critical Constraints (MUST follow)

### S08 SAFE Scope
- **NO streak display** (ST-903 deferred)
- **NO individual member breakdown** (requires ST-906 privacy)
- **NO privacy settings UI** (ST-906 deferred)
- Household progress = **aggregate only**
- User sees **only own progress**

### Non-Toxic Wording (MUST use)
| Element | Text |
|---------|------|
| Page title | "Progress" |
| Personal section | "Your Progress" |
| Household section | "Household Team Progress" |
| Household helper | "Totals across the whole household." |
| Empty state | "Start completing tasks to earn points!" |
| Encouragement | "Keep it up!" |
| Stats labels | "Total tasks completed", "Total points" |

---

## Task 1: ST-905 — Security Integration Tests

### File
`services/backend/src/test/java/com/hometusk/integration/GamificationSecurityIntegrationTest.java`

### Structure
```java
package com.hometusk.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.hometusk.households.domain.Household;
import com.hometusk.users.domain.Membership;
import com.hometusk.users.domain.MembershipRole;
import com.hometusk.users.domain.User;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

@DisplayName("Gamification Security Integration Tests — S08")
class GamificationSecurityIntegrationTest extends IntegrationTestBase {

    // Helper: add member to household
    private void addMember(User user, Household household, MembershipRole role) {
        Membership m = new Membership(user, household, role);
        membershipRepository.save(m);
    }

    // Helper: create task and complete it
    private UUID createTaskAndComplete(UUID householdId, User actor, UUID assigneeId) throws Exception {
        Map<String, Object> createCmd = Map.of(
            "householdId", householdId.toString(),
            "type", "create_task",
            "payload", Map.of("title", "Test task " + UUID.randomUUID(), "assigneeId", assigneeId.toString()),
            "source", "web");

        MvcResult result = mockMvc.perform(post("/api/v1/commands")
                .with(jwtForUser(actor))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createCmd)))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode resp = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID taskId = UUID.fromString(resp.get("result").get("taskId").asText());

        Map<String, Object> completeCmd = Map.of(
            "householdId", householdId.toString(),
            "type", "complete_task",
            "payload", Map.of("taskId", taskId.toString()),
            "source", "web");

        mockMvc.perform(post("/api/v1/commands")
                .with(jwtForUser(actor))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(completeCmd)))
            .andExpect(status().isOk());

        return taskId;
    }

    @Test
    @DisplayName("AC-1: Returns 403 for non-members")
    void getProgress_notMember_returns403() throws Exception {
        // testUser2 is NOT member of testHousehold
        mockMvc.perform(get("/api/v1/households/{id}/gamification/progress", testHousehold.getId())
                .with(jwtForUser(testUser2)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("AC-2: No cross-household data leak")
    void getProgress_differentHousehold_noDataLeak() throws Exception {
        // Create H1 task for testUser
        UUID taskH1 = createTaskAndComplete(testHousehold.getId(), testUser, testUser.getId());

        // Create H2 with testUser2
        Household household2 = householdRepository.save(new Household("Household Two"));
        addMember(testUser2, household2, MembershipRole.admin);
        createTaskAndComplete(household2.getId(), testUser2, testUser2.getId());

        // testUser requests H1 — should only see H1 data
        mockMvc.perform(get("/api/v1/households/{id}/gamification/progress", testHousehold.getId())
                .with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalPoints").value(10))
            .andExpect(jsonPath("$.householdTotalPoints").value(10))
            .andExpect(jsonPath("$.householdTotalTasks").value(1))
            .andExpect(jsonPath("$.recentActivity[0].taskId").value(taskH1.toString()));
    }

    @Test
    @DisplayName("AC-3: IDOR attempt returns 403 (not 404)")
    void getProgress_idorAttempt_returns403() throws Exception {
        UUID randomId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/households/{id}/gamification/progress", randomId)
                .with(jwt()))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("AC-4: User sees only own progress details")
    void getProgress_userSeesOnlyOwnProgress() throws Exception {
        // Add testUser2 to testHousehold
        addMember(testUser2, testHousehold, MembershipRole.member);

        // Both complete tasks
        UUID taskU1 = createTaskAndComplete(testHousehold.getId(), testUser, testUser.getId());
        createTaskAndComplete(testHousehold.getId(), testUser2, testUser2.getId());

        // testUser sees only own details
        mockMvc.perform(get("/api/v1/households/{id}/gamification/progress", testHousehold.getId())
                .with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(testUser.getId().toString()))
            .andExpect(jsonPath("$.totalPoints").value(10))
            .andExpect(jsonPath("$.pointsThisWeek").value(10))
            .andExpect(jsonPath("$.recentActivity.length()").value(1))
            .andExpect(jsonPath("$.recentActivity[0].taskId").value(taskU1.toString()))
            // But household aggregate includes all
            .andExpect(jsonPath("$.householdTotalPoints").value(20));
    }

    @Test
    @DisplayName("AC-5: Household aggregate includes all members")
    void getProgress_householdAggregateIncludesAllMembers() throws Exception {
        addMember(testUser2, testHousehold, MembershipRole.member);

        createTaskAndComplete(testHousehold.getId(), testUser, testUser.getId());
        createTaskAndComplete(testHousehold.getId(), testUser2, testUser2.getId());

        // Either member sees aggregate = 20 pts, 2 tasks
        mockMvc.perform(get("/api/v1/households/{id}/gamification/progress", testHousehold.getId())
                .with(jwt()))
            .andExpect(jsonPath("$.householdTotalPoints").value(20))
            .andExpect(jsonPath("$.householdTotalTasks").value(2));

        mockMvc.perform(get("/api/v1/households/{id}/gamification/progress", testHousehold.getId())
                .with(jwtForUser(testUser2)))
            .andExpect(jsonPath("$.householdTotalPoints").value(20))
            .andExpect(jsonPath("$.householdTotalTasks").value(2));
    }
}
```

---

## Task 2: ST-904 — Web Progress Page

### Step 1: Types
**File:** `clients/web/src/types/api.ts`

Add before `export type { Notification...`:
```typescript
// ============================================
// Gamification Types (ST-904)
// Aligned with backend DTOs: PointsEntryDto, BadgeDto, PointsReason
// ============================================

export type PointsReason = 'task_completed' | 'on_time_bonus' | 'task_uncompleted' | 'on_time_bonus_reversed';

export interface Badge {
  code: string;
  name: string;
  description: string;
  criteria: string;
  iconName: string;
  earned: boolean;
  earnedAt?: string;
}

export interface PointsEntry {
  id: string;
  taskId?: string;
  points: number;
  reason: PointsReason;
  createdAt: string;
}

export interface GamificationProgress {
  userId: string;
  totalPoints: number;
  pointsThisWeek: number;
  earnedBadges: Badge[];
  recentActivity: PointsEntry[];
  householdTotalTasks: number;
  householdTotalPoints: number;
}

export interface BadgeCatalogResponse {
  badges: Badge[];
}
```

### Step 2: API Functions
**File:** `clients/web/src/lib/api.ts`

Add import: `BadgeCatalogResponse, GamificationProgress`

Add at end:
```typescript
export async function getGamificationProgress(householdId: string): Promise<GamificationProgress> {
  return apiFetch<GamificationProgress>(`/households/${householdId}/gamification/progress`);
}

export async function getBadgeCatalog(householdId: string): Promise<BadgeCatalogResponse> {
  return apiFetch<BadgeCatalogResponse>(`/households/${householdId}/gamification/badges`);
}
```

### Step 3: Hook
**File:** `clients/web/src/hooks/useGamification.ts`

```typescript
import { useCallback, useEffect, useState } from 'react';
import { getBadgeCatalog, getGamificationProgress } from '../lib/api';
import type { BadgeCatalogResponse, GamificationProgress } from '../types/api';

export function useGamification(householdId: string | undefined) {
  const [progress, setProgress] = useState<GamificationProgress | null>(null);
  const [badges, setBadges] = useState<BadgeCatalogResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  const fetch = useCallback(async () => {
    if (!householdId) {
      setProgress(null);
      setBadges(null);
      setIsLoading(false);
      setError(null);
      return;
    }

    setIsLoading(true);
    setError(null);
    try {
      const [progressData, badgesData] = await Promise.all([
        getGamificationProgress(householdId),
        getBadgeCatalog(householdId),
      ]);
      setProgress(progressData);
      setBadges(badgesData);
    } catch (err) {
      setError(err instanceof Error ? err : new Error('Failed to load gamification data'));
    } finally {
      setIsLoading(false);
    }
  }, [householdId]);

  useEffect(() => {
    fetch();
  }, [fetch]);

  return { progress, badges, isLoading, error, refetch: fetch };
}
```

Export in `clients/web/src/hooks/index.ts`:
```typescript
export { useGamification } from './useGamification';
```

### Step 4: Components
**Directory:** `clients/web/src/components/gamification/`

#### BadgeGrid.tsx
```typescript
import type { Badge } from '../../types/api';

interface BadgeGridProps {
  badges: Badge[];
  title?: string;
  emptyLabel?: string;
}

// Map iconName from backend to emoji
const ICONS: Record<string, string> = {
  star: '⭐',
  trophy: '🏆',
  fire: '🔥',
  target: '🎯',
  clock: '⏰',
  check: '✅',
  lightning: '⚡',
};

export function BadgeGrid({ badges, title, emptyLabel = 'No badges yet' }: BadgeGridProps) {
  return (
    <div className="badge-grid">
      {title && <h3 className="badge-grid__title">{title}</h3>}
      {badges.length === 0 ? (
        <p className="badge-grid__empty">{emptyLabel}</p>
      ) : (
        <div className="badge-grid__items">
          {badges.map((badge) => (
            <div
              key={badge.code}
              className={`badge-card ${badge.earned ? 'badge-card--earned' : ''}`}
              title={badge.description}
            >
              <span className="badge-card__icon">{ICONS[badge.iconName] ?? '⭐'}</span>
              <span className="badge-card__name">{badge.name}</span>
              {badge.criteria && <span className="badge-card__criteria">{badge.criteria}</span>}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
```

#### PersonalProgressCard.tsx
```typescript
import type { Badge } from '../../types/api';
import { BadgeGrid } from './BadgeGrid';

interface PersonalProgressCardProps {
  totalPoints: number;
  pointsThisWeek: number;
  badges: Badge[];
  isEmpty: boolean;
}

export function PersonalProgressCard({
  totalPoints,
  pointsThisWeek,
  badges,
  isEmpty,
}: PersonalProgressCardProps) {
  const earnedBadges = badges.filter((b) => b.earned);

  return (
    <div className="progress__card personal-progress">
      <h2>Your Progress</h2>
      {isEmpty ? (
        <div className="progress__empty">
          <p>Start completing tasks to earn points!</p>
        </div>
      ) : (
        <>
          <div className="progress__stat progress__stat--primary">
            <span className="progress__stat-value">{totalPoints}</span>
            <span className="progress__stat-label">Total points</span>
          </div>
          <div className="progress__stat progress__stat--secondary">
            <span className="progress__stat-label">This week:</span>
            <span className="progress__stat-value">+{pointsThisWeek}</span>
          </div>
          <BadgeGrid badges={earnedBadges} title="Your Badges" emptyLabel="Complete tasks to earn badges!" />
          <p className="progress__encouragement">Keep it up!</p>
        </>
      )}
    </div>
  );
}
```

#### HouseholdAggregateCard.tsx
```typescript
interface HouseholdAggregateCardProps {
  householdTotalTasks: number;
  householdTotalPoints: number;
}

export function HouseholdAggregateCard({
  householdTotalTasks,
  householdTotalPoints,
}: HouseholdAggregateCardProps) {
  return (
    <div className="progress__card household-progress">
      <h2>Household Team Progress</h2>
      <p className="progress__helper">Totals across the whole household.</p>
      <div className="progress__stats">
        <div className="progress__stat">
          <span className="progress__stat-value">{householdTotalTasks}</span>
          <span className="progress__stat-label">Total tasks completed</span>
        </div>
        <div className="progress__stat">
          <span className="progress__stat-value">{householdTotalPoints}</span>
          <span className="progress__stat-label">Total points</span>
        </div>
      </div>
    </div>
  );
}
```

#### index.ts
```typescript
export { BadgeGrid } from './BadgeGrid';
export { PersonalProgressCard } from './PersonalProgressCard';
export { HouseholdAggregateCard } from './HouseholdAggregateCard';
```

### Step 5: Page
**File:** `clients/web/src/routes/Progress.tsx`

```typescript
import { Link, useParams } from 'react-router-dom';
import { HouseholdAggregateCard, PersonalProgressCard, BadgeGrid } from '../components/gamification';
import ErrorMessage from '../components/ui/ErrorMessage';
import Spinner from '../components/ui/Spinner';
import { useAuth } from '../hooks/useAuth';
import { useGamification } from '../hooks/useGamification';
import { ApiError } from '../lib/errors';
import './Progress.css';

export default function Progress() {
  const { householdId } = useAuth();
  const { householdId: paramId } = useParams();
  const activeId = paramId ?? householdId ?? undefined;

  const { progress, badges, isLoading, error, refetch } = useGamification(activeId);

  if (!activeId) {
    return (
      <div className="page progress">
        <h1>Progress</h1>
        <p>Select a household to view progress.</p>
      </div>
    );
  }

  if (error instanceof ApiError && error.status === 403) {
    return (
      <div className="page progress">
        <h1>Access Denied</h1>
        <p>You do not have access to this household.</p>
        <Link className="button" to="/households">Back to Household Selector</Link>
      </div>
    );
  }

  if (error) {
    return (
      <div className="page progress">
        <h1>Progress</h1>
        <ErrorMessage error={error} onRetry={refetch} />
      </div>
    );
  }

  if (isLoading || !progress) {
    return (
      <div className="page progress progress__loading">
        <h1>Progress</h1>
        <Spinner />
      </div>
    );
  }

  const isEmpty = progress.totalPoints === 0 && progress.earnedBadges.length === 0;

  return (
    <div className="page progress">
      <div className="progress__header">
        <h1>Progress</h1>
        <p className="progress__subtitle">Track your achievements and household team progress.</p>
      </div>

      <div className="progress__grid">
        <PersonalProgressCard
          totalPoints={progress.totalPoints}
          pointsThisWeek={progress.pointsThisWeek}
          badges={progress.earnedBadges}
          isEmpty={isEmpty}
        />

        <HouseholdAggregateCard
          householdTotalTasks={progress.householdTotalTasks}
          householdTotalPoints={progress.householdTotalPoints}
        />
      </div>

      {badges && badges.badges.length > 0 && (
        <div className="progress__catalog">
          <BadgeGrid badges={badges.badges} title="All Badges" emptyLabel="No badges available." />
        </div>
      )}
    </div>
  );
}
```

**File:** `clients/web/src/routes/Progress.css`

```css
/* Progress page layout */
.progress {
  display: flex;
  flex-direction: column;
  gap: 24px;
  max-width: 900px;
  margin: 0 auto;
}

.progress__header h1 {
  margin: 0 0 8px 0;
}

.progress__subtitle {
  color: var(--color-text-secondary);
  margin: 0;
}

.progress__grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.progress__card {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
  padding: 24px;
}

.progress__card h2 {
  font-size: var(--font-size-lg);
  margin: 0 0 16px 0;
}

.progress__helper {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  margin: 0 0 16px 0;
}

.progress__stats {
  display: flex;
  gap: 32px;
}

.progress__stat {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.progress__stat--primary .progress__stat-value {
  font-size: 48px;
  font-weight: var(--font-weight-bold);
}

.progress__stat--secondary {
  flex-direction: row;
  align-items: baseline;
  gap: 8px;
}

.progress__stat-value {
  font-size: 28px;
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.progress__stat-label {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.progress__encouragement {
  margin-top: 16px;
  font-weight: var(--font-weight-medium);
  color: var(--color-success);
}

.progress__empty {
  text-align: center;
  padding: 24px;
  color: var(--color-text-secondary);
}

.progress__loading {
  align-items: center;
}

.progress__catalog {
  margin-top: 8px;
}

/* Badge grid */
.badge-grid__title {
  font-size: var(--font-size-md);
  margin: 0 0 12px 0;
}

.badge-grid__empty {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.badge-grid__items {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.badge-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 12px 16px;
  border-radius: var(--radius-md);
  background: var(--color-bg-hover-subtle);
  opacity: 0.5;
  filter: grayscale(0.8);
  transition: all var(--transition-fast);
}

.badge-card--earned {
  opacity: 1;
  filter: none;
  background: linear-gradient(135deg, #FFF9C4 0%, #FFE082 100%);
}

.badge-card__icon {
  font-size: 24px;
}

.badge-card__name {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
}

.badge-card__criteria {
  font-size: 10px;
  color: var(--color-text-muted);
}

/* Responsive */
@media (max-width: 768px) {
  .progress__grid {
    grid-template-columns: 1fr;
  }

  .progress__stats {
    flex-direction: column;
    gap: 16px;
  }
}

@media (max-width: 480px) {
  .progress__card {
    padding: 16px;
  }

  .progress__stat--primary .progress__stat-value {
    font-size: 36px;
  }
}
```

### Step 6: Wire Routes
**File:** `clients/web/src/routes/index.tsx`

Add import:
```typescript
import Progress from './Progress';
```

Add route after analytics:
```typescript
{ path: 'progress', element: <Progress /> },
```

**File:** `clients/web/src/components/Layout/Sidebar.tsx`

Add after Analytics NavLink:
```tsx
<NavLink className={getLinkClass} to={`${basePath}/progress`}>
  Progress
</NavLink>
```

---

## Verification Commands
```bash
# Backend
cd services/backend
./gradlew test --tests "*GamificationSecurity*"
./gradlew spotlessApply

# Web
cd clients/web
npm run build
npm run lint
```

---

## DoD Checklist
- [ ] ST-905: 5 security tests pass
- [ ] ST-905: 403 for non-members
- [ ] ST-905: No cross-household leak
- [ ] ST-905: IDOR returns 403
- [ ] ST-904: Progress page renders at /progress
- [ ] ST-904: Personal card: points + this week + badges
- [ ] ST-904: Household card: total tasks + total points (all-time)
- [ ] ST-904: Empty state for 0 points
- [ ] ST-904: Sidebar link present
- [ ] ST-904: NO streak, NO individual breakdown
- [ ] Spotless applied
- [ ] Web builds without errors

---

## STOP-THE-LINE
If any of the following, STOP and report:
- Backend API differs from expected shape
- Required types/imports missing
- Existing tests fail
- Security boundaries unclear
