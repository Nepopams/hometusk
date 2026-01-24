# Epic: EP-009 — Gamification & Motivation v0

## Sources of Truth
- Initiative: `docs/planning/initiatives/INIT-2026Q2-gamification-motivation.md`
- Product Goal: `docs/planning/strategy/product-goal.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`
- OpenAPI: `docs/contracts/http/commands.openapi.yaml`

---

## Status
**Draft** — Awaiting Human Gate approval

## Initiative Alignment
This epic implements INIT-2026Q2-gamification-motivation:
- Points ledger for task completions
- Badges/achievements (milestone-based)
- Streaks with no-shame design (grace days, opt-out)
- Progress UI (profile + household view)

**Product Goal Pillar:** Fairness & Transparency (visible contributions via points)

---

## Epic Goal
Enable household members to:
1. **Earn points** for completing tasks (visible progress)
2. **Unlock badges** for milestones (10 tasks, 7-day streak, zone specialist)
3. **Track streaks** with no-shame design (grace days, best streak preserved)
4. **View progress** on dedicated Progress page (personal + household)

**Core principle:** Motivation through positive reinforcement, NOT shame or toxic competition.

---

## Outcome (User Value)
> "Закончил задачу → вижу +10 points → накопил 'Week Champion' бейдж → серия 5 дней (и если пропустил день — не обнуляется сразу благодаря grace day). Никто не ругает за отставание, только показывает прогресс."

---

## Non-Goals (Explicit)

| Item | Reason |
|------|--------|
| Global leaderboard | NO cross-household comparisons ever |
| Monetary rewards | Not a fintech product |
| ML/AI personalization | Deterministic rules only |
| Punitive mechanics | No negative points, no public shaming |
| Complex quests/storylines | Keep it simple for v0 |
| Kids mode / parental controls | Separate initiative |

---

## Key Decisions (SAFE S08 — Approved)

### A) Core Mechanic Set for SAFE S08
**Decision:** Points + Badges ONLY (no leaderboard; streaks deferred to S09)

Rationale: Validate value with minimal mechanics first. Avoid "streak pressure" early.

### B) Points Model
| Parameter | Decision |
|-----------|----------|
| Base points | 10 per completed task |
| On-time bonus | +5 if before deadline |
| Overdue penalty | **NO** (anti-shame) |
| Scope | Per-user with household aggregate |
| Reversible | Yes (task uncompleted = points subtracted) |
| Idempotency | Dedup by (taskId, reason) — no double-award |
| Config | Base/bonus values doc-level only (hardcoded in v0) |

### C) Streaks
**Decision:** DEFERRED to S09

ST-903 remains planned but OUT OF SCOPE for S08.

### D) Visibility (SAFE S08)
| Parameter | Decision |
|-----------|----------|
| S08 UI | "My progress" + "Household total" (aggregate only) |
| Individual breakdown | NOT in S08 (requires privacy toggle) |
| Privacy toggle | DEFERRED to S09 (ST-906) |

Rationale: No individual breakdown until privacy controls exist.

### E) Non-Toxic Wording Guidelines
| Avoid | Use Instead |
|-------|-------------|
| "Loser", "Last place" | "Still climbing!" |
| "You failed" | "Keep going!" |
| "X did more than you" | "Household total: Y tasks" |
| Rankings with positions | Progress bars, personal bests |

### F) Scope Boundaries (SAFE S08)
| Parameter | Decision |
|-----------|----------|
| Tasks only? | YES (shopping deferred) |
| Admin manual adjustment? | DEFERRED (no admin endpoint in S08) |
| Badge notifications? | YES (leverage EP-007) |

---

## Ethical Guardrails

### No-Shame Design Principles
1. **Streaks:** Grace day prevents anxiety; "best streak" not "broken streak"
2. **Points:** No negative points; no "you're behind" messaging
3. **Badges:** Earned, not lost; no "badge revoked" mechanics
4. **Comparisons:** Household-only, opt-in, framed as "team progress"
5. **Opt-out:** Any user can hide their gamification data from others

### Anti-Compulsion Measures
1. No push notifications for "don't break your streak!"
2. No artificial urgency ("complete now or lose points!")
3. Clear, predictable rules (no dark patterns)
4. User can disable gamification entirely

---

## Security & Privacy Checklist

| Check | Status | Notes |
|-------|--------|-------|
| Household boundary on all endpoints | Required | 403 if not member |
| No cross-household aggregation | Required | Single household per query |
| No IDOR on points/badges | Required | UUID + membership check |
| Privacy toggle respected | Required | Hidden users not shown |
| Audit log for manual adjustments | Required | Who/when/why |
| No PII in logs | Required | Only UUIDs |

---

## Data Model (Proposed)

### New Entities
```
PointsLedger
- id: UUID
- userId: UUID (FK)
- householdId: UUID (FK)
- taskId: UUID (FK, nullable for manual adj)
- points: int (can be negative for corrections)
- reason: enum (TASK_COMPLETED, TASK_UNCOMPLETED, ON_TIME_BONUS, MANUAL_ADJUSTMENT)
- createdAt: timestamp
- createdBy: UUID (for manual adj)
- note: string (for manual adj)

Badge (catalog)
- id: UUID
- code: string (unique, e.g., "FIRST_10_TASKS")
- name: string
- description: string
- criteria: string (human-readable)
- iconUrl: string (optional)

UserBadge
- id: UUID
- userId: UUID (FK)
- householdId: UUID (FK)
- badgeId: UUID (FK)
- earnedAt: timestamp

StreakState
- id: UUID
- userId: UUID (FK)
- householdId: UUID (FK)
- currentStreak: int
- bestStreak: int
- lastActivityDate: date
- graceUsedToday: boolean
- streakVisible: boolean (privacy toggle)
- updatedAt: timestamp

GamificationSettings (per user per household)
- id: UUID
- userId: UUID (FK)
- householdId: UUID (FK)
- showProgressToOthers: boolean (default true)
- gamificationEnabled: boolean (default true)
```

---

## API Contract (SAFE S08 — Minimal)

### S08 Endpoints (Committed)
```yaml
GET /api/v1/households/{householdId}/gamification/progress
  # Returns:
  #   - "my progress": userId, totalPoints, pointsThisWeek, earnedBadges[]
  #   - "household aggregate": totalTasks, totalPoints (no individual breakdown)

GET /api/v1/households/{householdId}/gamification/badges
  # Returns badge catalog + earned status for current user
```

### Deferred Endpoints (S09+)
```yaml
# DEFERRED: PUT /api/v1/households/{householdId}/gamification/settings
# DEFERRED: POST /api/v1/households/{householdId}/gamification/adjust
```

---

## Stories

| ID | Title | Status | Priority | S08 Scope | Points |
|----|-------|--------|----------|-----------|--------|
| ST-901 | Points Ledger + Task Completion | Draft | P1 | **Committed** | 5 |
| ST-902 | Badges v0 (5 milestones, streak-free) | Draft | P1 | **Committed** | 3 |
| ST-903 | Streaks v0 (grace day + opt-out) | Draft | P2 | Deferred S09 | 5 |
| ST-904 | Gamification UI (self + aggregate) | Draft | P1 | **Committed** | 5 |
| ST-905 | Security & Boundary Tests | Draft | P1 | **Committed** | 2 |
| ST-906 | Privacy Settings + Opt-out | Draft | P2 | Deferred S09 | 3 |

**S08 Committed:** 15 points (ST-901, ST-902, ST-904, ST-905)
**S09 Deferred:** 8 points (ST-903, ST-906)

### SAFE S08 Delivery
- **Sprint S08:** Points + Badges + Progress UI (self + household aggregate) + Security
- **Sprint S09:** Streaks + Privacy toggle + Individual breakdown

---

## Milestones

| Milestone | Stories | Exit Criteria |
|-----------|---------|---------------|
| M1: Points visible | ST-901, ST-904 | Complete task → points shown in UI |
| M2: Badges work | ST-902 | Unlock badge → notification + UI |
| M3: Streaks work | ST-903 | Daily streak tracked with grace |
| M4: Privacy | ST-906 | User can hide progress |

---

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Toxic competition | User conflict | No rankings, household-only, opt-out |
| Compulsive behavior | User stress | No urgency messaging, grace days |
| Privacy concerns | Trust loss | Explicit toggle, default-on visibility |
| Complexity creep | Scope bloat | Strict v0 boundaries |
| Gaming the system | Unfair points | Anti-cheat: no points for canceled tasks |

---

## Success Metrics

| Metric | Target |
|--------|--------|
| Points adoption | 60% users earn points in week 1 |
| Badge unlock rate | 50% users unlock ≥1 badge in 7 days |
| Streak engagement | 30% users maintain 3+ day streak |
| Opt-out rate | <10% disable gamification (healthy) |
| Conflict reports | 0 reports of "unfair" or "shaming" |

---

## Exit Criteria

1. Task completion → points added → visible in Progress page
2. 5 badges defined and auto-unlock working
3. Streak calculates correctly (including grace day)
4. Privacy toggle hides user from household view
5. Household boundary enforced (403 tests)
6. Non-toxic wording throughout UI
7. OpenAPI contract updated
8. Build passes, tests pass

---

## Flags

| Flag | Value | Notes |
|------|-------|-------|
| contract_impact | yes | New gamification endpoints |
| adr_needed | no | Standard patterns, no new architecture |
| diagrams_needed | no | No structural changes |
| security_sensitive | yes | Privacy toggles, household boundary |

---

## Related Artifacts

| Artifact | Path |
|----------|------|
| Initiative | `docs/planning/initiatives/INIT-2026Q2-gamification-motivation.md` |
| Sprint | `docs/planning/sprints/S08/sprint.md` |
| Workpack ST-901-902 | `docs/planning/workpacks/ST-901-902/` |
| Workpack ST-903 | `docs/planning/workpacks/ST-903/` |
| Workpack ST-904-905 | `docs/planning/workpacks/ST-904-905/` |
