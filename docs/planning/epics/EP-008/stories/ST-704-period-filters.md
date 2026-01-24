# Story: ST-704 — Period Filters

## Sources of Truth
- Epic: `docs/planning/epics/EP-008/epic.md`
- DoR: `docs/_governance/dor.md`

---

## Status
**Ready** — Sprint S07 stretch

## User Value
> "Хочу переключить период между 7 и 30 днями чтобы увидеть разную картину."

---

## Description
Add period filter toggle to analytics page:
- 7 days (default)
- 30 days
- URL reflects selection

---

## Acceptance Criteria

### AC-1: Period toggle visible
```
Given analytics page loaded
Then period toggle shows "7 days" / "30 days" options
And "7 days" selected by default
```

### AC-2: Switching period reloads data
```
Given "7 days" selected
When clicking "30 days"
Then loading state shown
And data refetched with period=30d
And UI updated with new data
```

### AC-3: URL reflects period
```
Given period=30d selected
Then URL updates to /analytics?period=30d
And page reload preserves selection
```

### AC-4: Visual feedback
```
Given period toggle
Then selected option is visually highlighted
And non-selected is visually distinct
```

---

## UI Component

```typescript
// PeriodToggle.tsx
interface PeriodToggleProps {
  value: '7d' | '30d';
  onChange: (period: '7d' | '30d') => void;
}
```

---

## Points
**2 points**

---

## Flags
- contract_impact: no
