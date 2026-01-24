# Sprint S08: Gamification & Motivation v0 (SAFE Scope)

## Sources of Truth
- Product Goal: `docs/planning/strategy/product-goal.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q2-gamification-motivation.md`
- Epic: `docs/planning/epics/EP-009/epic.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Draft** — Awaiting Human Gate approval

---

## Sprint Goal
Deliver **SAFE E2E thin slice**: user completes task → earns points → sees progress in UI. Points + Badges only (no streaks). Aggregate household view only (no individual breakdown until privacy exists).

---

## SAFE Thin Slice Definition
**"Complete task → earn 10 points → see points + badge progress on Progress page"**

This validates:
1. Points awarded on task completion (idempotent)
2. Badge criteria checked automatically (streak-free)
3. Progress page displays: "My Progress" + "Household Total"
4. Household boundary enforced
5. No-shame wording throughout

---

## Key Decisions (SAFE S08 — Approved)

| ID | Decision | Choice | Rationale |
|----|----------|--------|-----------|
| A | Core mechanics | Points + Badges only | Validate value before streaks |
| B | Points model | 10 base + 5 on-time, no penalty, idempotent | Anti-shame, no double-award |
| C | Streaks | **DEFERRED to S09** | Avoid "streak pressure" early |
| D | UI visibility | Self + Aggregate only | No individual breakdown without privacy |
| E | Non-toxic wording | Keep as defined | Progress framing, no rankings |
| F | Scope | Tasks only, no admin adjust | Keep minimal |

---

## Scope

### Committed (15 points)

| ID | Story | Points | Workpack |
|----|-------|--------|----------|
| ST-901 | Points Ledger + Task Completion | 5 | `workpacks/ST-901-902/` |
| ST-902 | Badges v0 (5 milestones, streak-free) | 3 | `workpacks/ST-901-902/` |
| ST-904 | Gamification UI (self + aggregate) | 5 | `workpacks/ST-904-905/` |
| ST-905 | Security & Boundary Tests | 2 | `workpacks/ST-904-905/` |

**Total Committed:** 15 points

### Explicitly Deferred (S09)

| ID | Story | Points | Reason |
|----|-------|--------|--------|
| ST-903 | Streaks v0 | 5 | Avoid streak pressure early |
| ST-906 | Privacy Settings | 3 | Required before individual breakdown |

**S09 Total:** 8 points

### Out of Scope (Explicit)

| Item | Reason |
|------|--------|
| Individual member breakdown | Requires privacy toggle (ST-906) |
| Streak display | Requires ST-903 |
| Household leaderboard | Avoid toxic competition |
| Admin manual adjustment | Deferred to future sprint |
| Shopping points | Tasks only for v0 |

---

## Dependencies

| Dependency | Status |
|------------|--------|
| EP-003 (Web Foundation) | Done |
| EP-004 (Auth) | Done |
| EP-005 (Household) | Done |
| EP-006 (Command UX) | Done |
| EP-007 (Notifications) | Done |
| Task completion event | Available |

---

## Delivery Sequence

```
Day 1-2: ST-901 + ST-902 (Backend Foundation)
         - PointsLedger entity + idempotency
         - PointsService (award, reverse, dedup)
         - Badge entity + catalog (5 streak-free)
         - BadgeService (check criteria)
         - Unit tests
         ↓
Day 3-4: ST-904 (Web UI)
         - ProgressPage
         - PersonalProgressCard (points, badges)
         - HouseholdAggregateCard (total only)
         - BadgeGrid
         - useGamification hook
         - API client
         ↓
Day 5:   ST-905 (Security Tests)
         - Boundary tests (5 tests)
         - IDOR prevention
         - User sees only own progress
```

---

## Contract Endpoints (S08 Minimal)

```yaml
GET /api/v1/households/{householdId}/gamification/progress
  # Response:
  #   userId, totalPoints, pointsThisWeek, earnedBadges[]
  #   householdTotalTasks, householdTotalPoints

GET /api/v1/households/{householdId}/gamification/badges
  # Response: badge catalog + earned status for current user
```

**DEFERRED:**
- `PUT /settings` (S09)
- `POST /adjust` (future)

---

## Ethical Guardrails (Non-negotiable)

### No-Shame Design
1. **No rankings** — "Team progress" not "leaderboard"
2. **No penalties** — No negative points for overdue
3. **No individual breakdown** — Until privacy exists (S09)
4. **Aggregate only** — Total household points, not "who did more"

### Forbidden Patterns
- "You're behind"
- "Last place"
- "X did more than you"
- Individual member points in household view

---

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Double point award | Data integrity | Idempotency by (taskId, reason) |
| Toxic comparison | User conflict | Aggregate only, no breakdown |
| Scope creep | Delay | Strict S08 boundaries |
| Badge criteria confusion | Wrong awards | Clear ledger-derived queries |

---

## Acceptance Criteria (Sprint-level)

### Core Flow
- [ ] Complete task → points added to ledger
- [ ] Same task completion → no double award (idempotent)
- [ ] Task uncomplete → full reversal (base + bonus)
- [ ] Progress page shows points, badges
- [ ] Badge unlocks when criteria met (5 streak-free badges)
- [ ] Household view shows **aggregate only**
- [ ] Non-members get 403

### Technical
- [ ] PointsLedger with idempotency key
- [ ] Badge catalog seeded (5 badges, ON_TIME_HERO not SEVEN_DAY_STREAK)
- [ ] Progress endpoint returns correct data
- [ ] Security tests pass (5 tests)
- [ ] OpenAPI contract updated
- [ ] Build passes (backend + web)

---

## Definition of Done (Sprint)

Sprint is **Done** when:
1. All committed stories pass DoD
2. Security tests pass (ST-905)
3. Manual QA: complete task → see points in UI
4. No cross-household data leaks
5. No individual breakdown visible
6. Non-toxic wording verified
7. Code review approved
8. Merged to main branch
9. Demo-ready

---

## Capacity Notes
- Backend: ~8 points (ST-901, ST-902)
- Web: ~5 points (ST-904)
- Tests: ~2 points (ST-905)

---

## Related Artifacts

| Artifact | Path |
|----------|------|
| Initiative | `docs/planning/initiatives/INIT-2026Q2-gamification-motivation.md` |
| Epic | `docs/planning/epics/EP-009/epic.md` |
| Workpack ST-901-902 | `docs/planning/workpacks/ST-901-902/` |
| Workpack ST-904-905 | `docs/planning/workpacks/ST-904-905/` |
| Workpack ST-903 | `docs/planning/workpacks/ST-903/` (deferred) |

---

## Next Steps After Approval

1. Update artifact statuses to "Ready"
2. Generate Codex PLAN prompts for ST-901-902
3. Begin backend implementation
4. Generate APPLY prompts after PLAN approval
5. Proceed to ST-904 after backend complete
6. Plan S09 with ST-903 + ST-906
