# Codex APPLY Prompt: ST-703 + ST-704 — Web Analytics Page + Period Filters

## Mode
**APPLY** — Implementation phase. File modifications allowed.

## Sources of Truth (AUTHORITATIVE)
1. `docs/planning/workpacks/ST-703-704/workpack.md` — Implementation plan
2. `docs/planning/epics/EP-008/stories/ST-703-web-analytics-page.md` — Story spec with ACs
3. `docs/planning/epics/EP-008/epic.md` — API contract schema
4. `docs/_governance/dod.md` — Definition of Done

## PLAN Phase Findings (Incorporated)

### Project Structure Verified
- Package manager: npm (Vite app)
- Data fetching: native fetch via `apiFetch` in `clients/web/src/lib/api.ts`
- Router: react-router-dom 6.16 with `createBrowserRouter` in `clients/web/src/routes/index.tsx`
- Styling: global CSS in `clients/web/src/styles/index.css` (no Tailwind/CSS modules)
- Household context: `clients/web/src/context/AuthContext.tsx` and `clients/web/src/hooks/useAuth.ts`

### Existing Patterns to Follow
- API functions: `clients/web/src/lib/api.ts` with types in `clients/web/src/types/api.ts`
- Data hooks: `useState`/`useEffect` returning `{ data, isLoading, error, refetch }` (see `useTasks.ts`, `useNotifications.ts`)
- Page structure: route components in `clients/web/src/routes/`, wrapped by `HouseholdLayout.tsx`
- Error/loading: `ErrorMessage.tsx` + `Spinner.tsx`; 403 handling with `ApiError`
- Navigation: sidebar links in `Sidebar.tsx` using `NavLink`

---

## Human Gate Decisions

| Question | Decision |
|----------|----------|
| Include ST-704 period toggle? | **Yes** — include toggle + URL query param sync |
| Page location | `clients/web/src/routes/Analytics.tsx` (follow existing pattern) |
| Types/API location | Add to existing `types/api.ts` and `lib/api.ts` |
| Tests | **Skip** — no test framework in deps |

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

### 2. Balance Score Display Rules
- Value: large number or "N/A" if null
- Color classes:
  - `.balance-excellent` (green): balance ≥ 70
  - `.balance-moderate` (yellow): balance 50-69
  - `.balance-low` (red): balance < 50
  - `.balance-na` (gray): balance is null
- Interpretation: text from API response (`fairness.interpretation`)
- Expandable: "How is this calculated?" → show `fairness.formula`

### 3. API Contract (Backend Already Implemented)
```typescript
GET /api/v1/households/{householdId}/analytics?period=7d|30d

Response: {
  householdId: string;
  period: '7d' | '30d';
  periodStart: string;  // ISO datetime
  periodEnd: string;    // ISO datetime
  perMember: MemberStats[];
  perZone: ZoneStats[];
  fairness: FairnessInfo;
  overdueTop?: OverdueTask[];  // max 5, sorted by daysOverdue desc
}
```

---

## Implementation Plan

### Step 1: Add TypeScript Interfaces
**File:** `clients/web/src/types/api.ts`

Add at end of file:
```typescript
// Analytics types (ST-703/ST-704)
export type AnalyticsPeriod = '7d' | '30d';

export interface MemberStats {
  memberId: string;
  memberName: string;
  completedCount: number;
  overdueCount: number;
  openCount: number;
}

export interface ZoneStats {
  zoneId: string;
  zoneName: string;
  completedCount: number;
  overdueCount: number;
}

export interface FairnessInfo {
  gini: number | null;
  balance: number | null;
  formula: string;
  interpretation: string;
}

export interface OverdueTask {
  taskId: string;
  title: string;
  assigneeName: string;
  daysOverdue: number;
}

export interface AnalyticsSummary {
  householdId: string;
  period: AnalyticsPeriod;
  periodStart: string;
  periodEnd: string;
  perMember: MemberStats[];
  perZone: ZoneStats[];
  fairness: FairnessInfo;
  overdueTop?: OverdueTask[];
}
```

### Step 2: Add API Function
**File:** `clients/web/src/lib/api.ts`

Add function (follow existing pattern):
```typescript
export async function getAnalytics(
  householdId: string,
  period: AnalyticsPeriod = '7d'
): Promise<AnalyticsSummary> {
  return apiFetch(`/api/v1/households/${householdId}/analytics?period=${period}`);
}
```

### Step 3: Create useAnalytics Hook
**File:** `clients/web/src/hooks/useAnalytics.ts`

Follow `useTasks.ts` pattern:
```typescript
import { useState, useEffect, useCallback } from 'react';
import { getAnalytics } from '../lib/api';
import { AnalyticsSummary, AnalyticsPeriod } from '../types/api';

export function useAnalytics(householdId: string | undefined, period: AnalyticsPeriod = '7d') {
  const [data, setData] = useState<AnalyticsSummary | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  const fetchData = useCallback(async () => {
    if (!householdId) return;

    setIsLoading(true);
    setError(null);

    try {
      const result = await getAnalytics(householdId, period);
      setData(result);
    } catch (err) {
      setError(err instanceof Error ? err : new Error('Failed to fetch analytics'));
    } finally {
      setIsLoading(false);
    }
  }, [householdId, period]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  return { data, isLoading, error, refetch: fetchData };
}
```

### Step 4: Create Analytics Components
**Directory:** `clients/web/src/components/analytics/`

#### 4.1 BalanceScoreCard.tsx
```typescript
import { useState } from 'react';
import { FairnessInfo } from '../../types/api';

interface Props {
  fairness: FairnessInfo;
}

function getBalanceClass(balance: number | null): string {
  if (balance === null) return 'balance-na';
  if (balance >= 70) return 'balance-excellent';
  if (balance >= 50) return 'balance-moderate';
  return 'balance-low';
}

export function BalanceScoreCard({ fairness }: Props) {
  const [expanded, setExpanded] = useState(false);
  const { balance, interpretation, formula } = fairness;

  return (
    <div className="card balance-card">
      <h3>Balance Score</h3>
      <div className={`balance-value ${getBalanceClass(balance)}`}>
        {balance !== null ? balance : 'N/A'}
      </div>
      <p className="balance-interpretation">{interpretation}</p>
      <button
        className="balance-toggle"
        onClick={() => setExpanded(!expanded)}
      >
        {expanded ? 'Hide calculation' : 'How is this calculated?'}
      </button>
      {expanded && (
        <div className="balance-formula">
          <code>{formula}</code>
        </div>
      )}
    </div>
  );
}
```

#### 4.2 MemberStatsList.tsx
```typescript
import { MemberStats } from '../../types/api';

interface Props {
  members: MemberStats[];
}

export function MemberStatsList({ members }: Props) {
  if (members.length === 0) {
    return <p className="empty-state">No member data available</p>;
  }

  return (
    <div className="card">
      <h3>Member Contributions</h3>
      <table className="stats-table">
        <thead>
          <tr>
            <th>Member</th>
            <th>Completed</th>
            <th>Open</th>
            <th>Overdue</th>
          </tr>
        </thead>
        <tbody>
          {members.map((member) => (
            <tr key={member.memberId}>
              <td>{member.memberName}</td>
              <td>{member.completedCount}</td>
              <td>{member.openCount}</td>
              <td className={member.overdueCount > 0 ? 'overdue-highlight' : ''}>
                {member.overdueCount}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
```

#### 4.3 ZoneStatsList.tsx
```typescript
import { ZoneStats } from '../../types/api';

interface Props {
  zones: ZoneStats[];
}

export function ZoneStatsList({ zones }: Props) {
  if (zones.length === 0) {
    return <p className="empty-state">No zone data available</p>;
  }

  return (
    <div className="card">
      <h3>Zone Breakdown</h3>
      <table className="stats-table">
        <thead>
          <tr>
            <th>Zone</th>
            <th>Completed</th>
            <th>Overdue</th>
          </tr>
        </thead>
        <tbody>
          {zones.map((zone) => (
            <tr key={zone.zoneId}>
              <td>{zone.zoneName}</td>
              <td>{zone.completedCount}</td>
              <td className={zone.overdueCount > 0 ? 'overdue-highlight' : ''}>
                {zone.overdueCount}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
```

#### 4.4 OverdueTasksList.tsx
```typescript
import { OverdueTask } from '../../types/api';

interface Props {
  tasks?: OverdueTask[];
}

export function OverdueTasksList({ tasks }: Props) {
  if (!tasks || tasks.length === 0) {
    return (
      <div className="card">
        <h3>Overdue Tasks</h3>
        <p className="empty-state">No overdue tasks</p>
      </div>
    );
  }

  return (
    <div className="card">
      <h3>Overdue Tasks (Top 5)</h3>
      <ul className="overdue-list">
        {tasks.map((task) => (
          <li key={task.taskId} className="overdue-item">
            <span className="overdue-title">{task.title}</span>
            <span className="overdue-assignee">{task.assigneeName}</span>
            <span className="overdue-days">{task.daysOverdue} day{task.daysOverdue !== 1 ? 's' : ''} overdue</span>
          </li>
        ))}
      </ul>
    </div>
  );
}
```

#### 4.5 PeriodToggle.tsx (ST-704)
```typescript
import { AnalyticsPeriod } from '../../types/api';

interface Props {
  period: AnalyticsPeriod;
  onChange: (period: AnalyticsPeriod) => void;
}

export function PeriodToggle({ period, onChange }: Props) {
  return (
    <div className="period-toggle">
      <button
        className={`period-btn ${period === '7d' ? 'active' : ''}`}
        onClick={() => onChange('7d')}
      >
        Last 7 days
      </button>
      <button
        className={`period-btn ${period === '30d' ? 'active' : ''}`}
        onClick={() => onChange('30d')}
      >
        Last 30 days
      </button>
    </div>
  );
}
```

#### 4.6 index.ts (barrel exports)
```typescript
export { BalanceScoreCard } from './BalanceScoreCard';
export { MemberStatsList } from './MemberStatsList';
export { ZoneStatsList } from './ZoneStatsList';
export { OverdueTasksList } from './OverdueTasksList';
export { PeriodToggle } from './PeriodToggle';
```

### Step 5: Create Analytics Page
**File:** `clients/web/src/routes/Analytics.tsx`

```typescript
import { useState } from 'react';
import { useParams, useSearchParams } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { useAnalytics } from '../hooks/useAnalytics';
import { AnalyticsPeriod } from '../types/api';
import { Spinner } from '../components/ui/Spinner';
import { ErrorMessage } from '../components/ui/ErrorMessage';
import {
  BalanceScoreCard,
  MemberStatsList,
  ZoneStatsList,
  OverdueTasksList,
  PeriodToggle,
} from '../components/analytics';

export function Analytics() {
  const { householdId } = useParams<{ householdId: string }>();
  const [searchParams, setSearchParams] = useSearchParams();
  const { currentHousehold } = useAuth();

  // ST-704: Period from URL query param with fallback
  const periodParam = searchParams.get('period');
  const period: AnalyticsPeriod = periodParam === '30d' ? '30d' : '7d';

  const { data, isLoading, error } = useAnalytics(householdId, period);

  const handlePeriodChange = (newPeriod: AnalyticsPeriod) => {
    setSearchParams({ period: newPeriod });
  };

  if (isLoading) {
    return (
      <div className="page analytics-page">
        <Spinner />
      </div>
    );
  }

  if (error) {
    return (
      <div className="page analytics-page">
        <ErrorMessage message={error.message} />
      </div>
    );
  }

  if (!data) {
    return (
      <div className="page analytics-page">
        <ErrorMessage message="No analytics data available" />
      </div>
    );
  }

  return (
    <div className="page analytics-page">
      <header className="page-header">
        <h1>Analytics</h1>
        <PeriodToggle period={period} onChange={handlePeriodChange} />
      </header>

      <div className="analytics-grid">
        <BalanceScoreCard fairness={data.fairness} />
        <MemberStatsList members={data.perMember} />
        <ZoneStatsList zones={data.perZone} />
        <OverdueTasksList tasks={data.overdueTop} />
      </div>

      <footer className="analytics-footer">
        <small>
          Period: {new Date(data.periodStart).toLocaleDateString()} — {new Date(data.periodEnd).toLocaleDateString()}
        </small>
      </footer>
    </div>
  );
}
```

### Step 6: Add Route
**File:** `clients/web/src/routes/index.tsx`

Add import at top:
```typescript
import { Analytics } from './Analytics';
```

Add route as child of household layout (find the `/households/:householdId` route):
```typescript
{
  path: 'analytics',
  element: <Analytics />,
},
```

### Step 7: Add Navigation Link
**File:** `clients/web/src/components/Layout/Sidebar.tsx`

Add NavLink (follow existing pattern, place after Tasks or similar):
```typescript
<NavLink to={`/households/${householdId}/analytics`} className={linkClass}>
  Analytics
</NavLink>
```

### Step 8: Add CSS Styles
**File:** `clients/web/src/styles/index.css`

Add at end of file:
```css
/* Analytics Page (ST-703/ST-704) */
.analytics-page {
  padding: 1.5rem;
}

.analytics-page .page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.analytics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 1.5rem;
}

/* Balance Score Card */
.balance-card {
  text-align: center;
}

.balance-value {
  font-size: 3rem;
  font-weight: bold;
  margin: 1rem 0;
}

.balance-excellent {
  color: #22c55e;
}

.balance-moderate {
  color: #eab308;
}

.balance-low {
  color: #ef4444;
}

.balance-na {
  color: #9ca3af;
}

.balance-interpretation {
  color: #6b7280;
  margin-bottom: 1rem;
}

.balance-toggle {
  background: none;
  border: none;
  color: #3b82f6;
  cursor: pointer;
  text-decoration: underline;
  font-size: 0.875rem;
}

.balance-formula {
  margin-top: 0.75rem;
  padding: 0.75rem;
  background: #f3f4f6;
  border-radius: 0.375rem;
}

.balance-formula code {
  font-size: 0.875rem;
}

/* Stats Tables */
.stats-table {
  width: 100%;
  border-collapse: collapse;
}

.stats-table th,
.stats-table td {
  padding: 0.75rem;
  text-align: left;
  border-bottom: 1px solid #e5e7eb;
}

.stats-table th {
  font-weight: 600;
  color: #374151;
}

.overdue-highlight {
  color: #ef4444;
  font-weight: 600;
}

/* Overdue Tasks List */
.overdue-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.overdue-item {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  padding: 0.75rem 0;
  border-bottom: 1px solid #e5e7eb;
}

.overdue-item:last-child {
  border-bottom: none;
}

.overdue-title {
  flex: 1;
  font-weight: 500;
}

.overdue-assignee {
  color: #6b7280;
}

.overdue-days {
  color: #ef4444;
  font-size: 0.875rem;
}

/* Period Toggle (ST-704) */
.period-toggle {
  display: flex;
  gap: 0.5rem;
}

.period-btn {
  padding: 0.5rem 1rem;
  border: 1px solid #d1d5db;
  border-radius: 0.375rem;
  background: white;
  cursor: pointer;
  transition: all 0.15s;
}

.period-btn:hover {
  background: #f3f4f6;
}

.period-btn.active {
  background: #3b82f6;
  color: white;
  border-color: #3b82f6;
}

/* Empty States */
.empty-state {
  color: #9ca3af;
  text-align: center;
  padding: 2rem;
}

/* Analytics Footer */
.analytics-footer {
  margin-top: 1.5rem;
  text-align: center;
  color: #9ca3af;
}
```

---

## Verification Commands

After implementation, run:
```bash
# Build check
cd clients/web && npm run build

# Dev server (manual verification)
cd clients/web && npm run dev
# Then navigate to /households/{id}/analytics
```

### Manual Verification Checklist
- [ ] Analytics page loads at `/households/:householdId/analytics`
- [ ] Balance score displays with correct color (green/yellow/red/gray)
- [ ] "How is this calculated?" expands to show formula
- [ ] Member stats table shows all household members
- [ ] Zone stats table shows all zones with tasks
- [ ] Overdue tasks list shows max 5 tasks
- [ ] Period toggle switches between 7d and 30d
- [ ] URL updates with `?period=30d` when toggled
- [ ] Page reload preserves period selection
- [ ] 403 error handled gracefully (non-member access)
- [ ] Loading spinner shows during fetch
- [ ] Error message shows on API failure
- [ ] Non-toxic wording used throughout (no "fairness", "unfair", etc.)

---

## Acceptance Criteria Mapping

| AC | Implementation |
|----|----------------|
| ST-703 AC-1: Page accessible | Route added, Sidebar link added |
| ST-703 AC-2: Member contributions | MemberStatsList component |
| ST-703 AC-3: Zone breakdown | ZoneStatsList component |
| ST-703 AC-4: Balance score | BalanceScoreCard with colors + expandable |
| ST-703 AC-5: Overdue tasks | OverdueTasksList component (max 5) |
| ST-703 AC-6: Non-toxic wording | All labels use neutral language |
| ST-704 AC-1: Period toggle | PeriodToggle component |
| ST-704 AC-2: URL sync | useSearchParams for period param |
| ST-704 AC-3: Default 7d | Fallback in Analytics.tsx |

---

## Forbidden (Do NOT)
- Do NOT modify backend files (`services/backend/**`)
- Do NOT add new npm dependencies
- Do NOT create test files (no test framework)
- Do NOT use "fairness score", "unfair", "winner/loser" in UI text

## Stop Conditions
If any of these occur, STOP and describe:
- Import errors (missing types/components)
- Build failures
- Router configuration conflicts
- CSS class name conflicts
