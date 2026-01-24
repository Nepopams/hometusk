# Workpack: ST-703 + ST-704 — Web Analytics Page + Filters

## Sources of Truth
- Epic: `docs/planning/epics/EP-008/epic.md`
- Stories: `docs/planning/epics/EP-008/stories/ST-703-web-analytics-page.md`, `docs/planning/epics/EP-008/stories/ST-704-period-filters.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q2-analytics-fairness-dashboard.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Ready** — ST-703 committed, ST-704 stretch

---

## Outcome
Web analytics page showing member contributions, zone breakdown, overdue tasks, and balance score with 7d/30d toggle. Non-toxic wording throughout.

---

## Scope

### In Scope (ST-703 — Committed)
- `/households/{id}/analytics` route
- Member stats display (completed, open, overdue)
- Zone stats display
- Overdue tasks section
- Balance score with expandable explanation
- Navigation link
- Error handling
- Loading states

### In Scope (ST-704 — Stretch)
- Period toggle (7d/30d)
- URL query param sync

### Out of Scope
- Custom date picker
- Charts/graphs (simple lists for v0)
- Export/download
- Sorting/filtering within lists

---

## Files to Change/Create

### New Files
| Path | Purpose |
|------|---------|
| `clients/web/src/pages/AnalyticsPage.tsx` | Main analytics page |
| `clients/web/src/components/analytics/MemberStatsList.tsx` | Member breakdown |
| `clients/web/src/components/analytics/ZoneStatsList.tsx` | Zone breakdown |
| `clients/web/src/components/analytics/OverdueTasksList.tsx` | Overdue highlight |
| `clients/web/src/components/analytics/BalanceScoreCard.tsx` | Balance score display |
| `clients/web/src/components/analytics/PeriodToggle.tsx` | Period filter (ST-704) |
| `clients/web/src/hooks/useAnalytics.ts` | Data fetching hook |
| `clients/web/src/api/analytics.ts` | API client functions |
| `clients/web/src/__tests__/pages/AnalyticsPage.test.tsx` | Page tests |
| `clients/web/src/__tests__/components/analytics/BalanceScoreCard.test.tsx` | Component tests |

### Modified Files
| Path | Changes |
|------|---------|
| `clients/web/src/router.tsx` | Add /analytics route |
| `clients/web/src/components/layout/HouseholdNav.tsx` | Add Analytics link |

---

## Implementation Plan

### Step 1: Add API Client
```typescript
// src/api/analytics.ts
import { api } from './client';

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
  period: '7d' | '30d';
  periodStart: string;
  periodEnd: string;
  perMember: MemberStats[];
  perZone: ZoneStats[];
  fairness: FairnessInfo;
  overdueTop?: OverdueTask[];
}

export async function getAnalytics(
  householdId: string,
  period: '7d' | '30d' = '7d'
): Promise<AnalyticsSummary> {
  const response = await api.get(`/households/${householdId}/analytics`, {
    params: { period },
  });
  return response.data;
}
```

### Step 2: Create useAnalytics Hook
```typescript
// src/hooks/useAnalytics.ts
import { useQuery } from '@tanstack/react-query';
import { getAnalytics, AnalyticsSummary } from '../api/analytics';

export function useAnalytics(householdId: string, period: '7d' | '30d') {
  return useQuery<AnalyticsSummary>({
    queryKey: ['analytics', householdId, period],
    queryFn: () => getAnalytics(householdId, period),
    staleTime: 30_000, // 30 seconds
  });
}
```

### Step 3: Create BalanceScoreCard Component
```typescript
// src/components/analytics/BalanceScoreCard.tsx
interface BalanceScoreCardProps {
  fairness: FairnessInfo;
}

export function BalanceScoreCard({ fairness }: BalanceScoreCardProps) {
  const [expanded, setExpanded] = useState(false);

  const getColorClass = (balance: number | null) => {
    if (balance === null) return 'text-gray-500';
    if (balance >= 70) return 'text-green-600';
    if (balance >= 50) return 'text-yellow-600';
    return 'text-red-600';
  };

  return (
    <div className="bg-white rounded-lg p-4 shadow">
      <h3 className="text-lg font-medium mb-2">Balance Score</h3>
      <div className={`text-4xl font-bold ${getColorClass(fairness.balance)}`}>
        {fairness.balance ?? 'N/A'}
      </div>
      <p className="text-sm text-gray-600 mt-2">{fairness.interpretation}</p>
      <button
        onClick={() => setExpanded(!expanded)}
        className="text-sm text-blue-600 mt-2"
      >
        {expanded ? 'Hide details' : 'How is this calculated?'}
      </button>
      {expanded && (
        <div className="mt-2 p-2 bg-gray-50 rounded text-sm">
          <p><strong>Formula:</strong> {fairness.formula}</p>
          <p className="mt-1">
            The Gini coefficient measures inequality in task distribution.
            A score of 100 means tasks are evenly distributed.
          </p>
        </div>
      )}
    </div>
  );
}
```

### Step 4: Create MemberStatsList Component
```typescript
// src/components/analytics/MemberStatsList.tsx
interface MemberStatsListProps {
  members: MemberStats[];
}

export function MemberStatsList({ members }: MemberStatsListProps) {
  return (
    <div className="bg-white rounded-lg p-4 shadow">
      <h3 className="text-lg font-medium mb-4">Members</h3>
      <div className="space-y-3">
        {members.map((member) => (
          <div key={member.memberId} className="flex items-center justify-between">
            <span className="font-medium">{member.memberName}</span>
            <div className="flex gap-4 text-sm">
              <span className="text-green-600">✓ {member.completedCount}</span>
              <span className="text-gray-500">○ {member.openCount}</span>
              {member.overdueCount > 0 && (
                <span className="text-red-600">⚠ {member.overdueCount}</span>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
```

### Step 5: Create AnalyticsPage
```typescript
// src/pages/AnalyticsPage.tsx
export function AnalyticsPage() {
  const { householdId } = useParams<{ householdId: string }>();
  const [period, setPeriod] = useState<'7d' | '30d'>('7d');

  const { data, isLoading, error } = useAnalytics(householdId!, period);

  if (isLoading) return <LoadingSpinner />;
  if (error) return <ErrorMessage error={error} />;
  if (!data) return null;

  return (
    <div className="container mx-auto p-4">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Analytics</h1>
        <PeriodToggle value={period} onChange={setPeriod} />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
        <BalanceScoreCard fairness={data.fairness} />
        <OverdueTasksList tasks={data.overdueTop ?? []} />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <MemberStatsList members={data.perMember} />
        <ZoneStatsList zones={data.perZone} />
      </div>
    </div>
  );
}
```

### Step 6: Add Route
```typescript
// In router.tsx
{ path: '/households/:householdId/analytics', element: <AnalyticsPage /> }
```

### Step 7: Add Navigation Link
```typescript
// In HouseholdNav.tsx
<NavLink to={`/households/${householdId}/analytics`}>Analytics</NavLink>
```

### Step 8: Write Tests

---

## Verification Commands

```bash
cd /home/vad/Документы/hometusk/clients/web

# Install deps (if needed)
npm install

# Type check
npm run typecheck

# Lint
npm run lint

# Tests
npm test -- --watchAll=false

# Build
npm run build

# Dev server
npm run dev
```

---

## Non-Toxic Wording Guidelines

| Avoid | Use Instead |
|-------|-------------|
| "Fairness score" | "Balance score" |
| "Winner/loser" | "Distribution" |
| "X did less" (blame) | "X completed N tasks" (neutral) |
| "Unfair" | "Imbalanced" |
| "Failed to complete" | "Open tasks" |

---

## Risks
| Risk | Mitigation |
|------|------------|
| Backend not ready | Mock API responses for dev |
| Many members (>10) | Scroll container, no pagination v0 |
| Null balance confusion | Clear "N/A" + explanation |

---

## Rollback
- Remove AnalyticsPage
- Remove route
- Remove navigation link
- Users see 404 (acceptable for unreleased feature)

---

## DoD Checklist
See `checklist.md`
