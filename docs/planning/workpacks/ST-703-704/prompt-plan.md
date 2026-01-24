# Codex PLAN Prompt: ST-703 — Web Analytics Page

## Mode
**PLAN ONLY** — Read-only exploration. NO file modifications allowed.

## Allowed Commands (Whitelist)
```
ls, find, cat, rg, grep, sed -n, head, tail, git status, git diff
```

## Forbidden
- Any file edits/writes/moves/deletes
- Network access
- Package install (npm install)
- npm run build/dev
- git commit/push

---

## Task
Plan the implementation of Web Analytics Page displaying member contributions, zone breakdown, overdue tasks, and Gini-based balance score.

## Sources of Truth (MUST READ)
1. `docs/planning/workpacks/ST-703-704/workpack.md` — Implementation plan (AUTHORITATIVE)
2. `docs/planning/epics/EP-008/stories/ST-703-web-analytics-page.md` — Story spec with ACs
3. `docs/planning/epics/EP-008/epic.md` — API contract schema
4. `clients/web/src/router.tsx` — Existing routing
5. `clients/web/src/api/` — Existing API client patterns
6. `clients/web/src/hooks/` — Existing hooks patterns
7. `clients/web/src/components/` — Existing component patterns
8. `clients/web/src/pages/` — Existing page patterns

---

## Critical Constraints (MUST FOLLOW)

### 1. Non-Toxic Wording (Required)
| Avoid | Use Instead |
|-------|-------------|
| "Fairness score" | "Balance score" |
| "Winner/loser" | "Distribution" |
| "X did less" (blame) | "X completed N tasks" (neutral) |
| "Unfair" | "Imbalanced" |
| "Failed to complete" | "Open tasks" |

### 2. Balance Score Display
- Value: large number or "N/A" if null
- Color: green (≥70), yellow (50-69), red (<50), gray (null)
- Interpretation: text from API response
- Expandable: "How is this calculated?" → show formula

### 3. API Contract (Already Implemented)
```typescript
GET /api/v1/households/{householdId}/analytics?period=7d|30d

Response: {
  householdId: string;
  period: '7d' | '30d';
  periodStart: string;
  periodEnd: string;
  perMember: MemberStats[];
  perZone: ZoneStats[];
  fairness: FairnessInfo;
  overdueTop?: OverdueTask[];
}
```

### 4. Existing Patterns to Follow
- Use `@tanstack/react-query` for data fetching (if already used)
- Use existing API client pattern
- Use existing component styling (Tailwind or CSS modules)
- Use existing layout/nav patterns

---

## Exploration Tasks

### Task 1: Understand Project Structure
- Read `clients/web/package.json` — dependencies (react-query? axios? fetch?)
- Read `clients/web/src/router.tsx` — how routes are defined
- Read `clients/web/src/api/client.ts` or similar — API client pattern

### Task 2: Understand Existing Pages
- Read any existing page (e.g., `TasksPage.tsx`, `HomePage.tsx`)
- How are pages structured?
- How is data fetched (useQuery? useEffect+useState?)
- How are loading/error states handled?

### Task 3: Understand Existing Components
- Read `clients/web/src/components/` structure
- Are there shared layout components?
- What styling approach is used (Tailwind classes? CSS modules?)
- Are there reusable cards/lists?

### Task 4: Understand Hooks Pattern
- Read `clients/web/src/hooks/` — are there existing data hooks?
- What patterns are used (useQuery wrapper? custom hooks?)

### Task 5: Understand Navigation
- Read `clients/web/src/components/layout/` — navigation components
- How are nav links added?
- Is there HouseholdNav or similar?

### Task 6: Check for Existing Analytics/Notifications Patterns
- Are there similar list components (NotificationList, TaskList)?
- Are there card components that can be reused?

---

## Files Expected to Create
| Path | Purpose |
|------|---------|
| `clients/web/src/pages/AnalyticsPage.tsx` | Main analytics page |
| `clients/web/src/components/analytics/MemberStatsList.tsx` | Member breakdown list |
| `clients/web/src/components/analytics/ZoneStatsList.tsx` | Zone breakdown list |
| `clients/web/src/components/analytics/OverdueTasksList.tsx` | Overdue tasks section |
| `clients/web/src/components/analytics/BalanceScoreCard.tsx` | Balance score with expandable |
| `clients/web/src/components/analytics/index.ts` | Barrel exports |
| `clients/web/src/hooks/useAnalytics.ts` | Data fetching hook |
| `clients/web/src/api/analytics.ts` | API client functions |

## Files Expected to Modify
| Path | Change |
|------|--------|
| `clients/web/src/router.tsx` | Add /households/:householdId/analytics route |
| `clients/web/src/components/layout/HouseholdNav.tsx` (or similar) | Add Analytics nav link |

## Forbidden Paths (Do NOT Touch)
- Backend files (`services/backend/**`)
- `docs/contracts/**` (already done)
- `package.json` (no new deps without approval)

---

## Output Format

After exploration, provide:

### 1. Verification of Project Setup
- Package manager (npm/pnpm/yarn)
- Key dependencies (react-query, axios, etc.)
- Styling approach (Tailwind, CSS modules, styled-components)
- Router library (react-router-dom version)

### 2. Existing Patterns to Follow
- API client pattern (how to call backend)
- Data fetching pattern (useQuery vs useEffect)
- Page structure pattern
- Component structure pattern
- Navigation pattern

### 3. Files to Create (with key interfaces/components)
List each file with:
- Purpose
- Key exports
- Dependencies on existing code

### 4. Files to Modify (with specific changes)
List each file with:
- What to add
- Where to add it

### 5. TypeScript Interfaces Needed
Based on API contract, list interfaces to define:
- AnalyticsSummary
- MemberStats
- ZoneStats
- FairnessInfo
- OverdueTask

### 6. Risks/Blockers
- Missing dependencies?
- Conflicting patterns?
- Unknown project structure?

### 7. Questions (if any)
- Clarifications needed before APPLY phase

---

## Stop Conditions

If any of these occur, STOP and describe:
- No react-query or similar (need different data fetching)
- No existing API client pattern
- Unknown router configuration
- Styling approach unclear
- Missing household context provider

Do NOT guess without evidence from codebase.
