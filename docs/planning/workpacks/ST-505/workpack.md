# Workpack: ST-505 — Minimal Trace Viewer

## Sources of Truth
- Story: `docs/planning/epics/EP-006/stories/ST-505-trace-viewer.md`
- Epic: `docs/planning/epics/EP-006/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q2-command-ux.md`
- Product Goal: `docs/planning/strategy/product-goal.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`
- Existing TraceInfo: `clients/web/src/components/commands/TraceInfo.tsx`

---

## Goal
Enhance trace display with expandable details view, raw JSON viewer, and reusable copy functionality.

## User Value
As a household member (or support person), I want to see trace information for a command, so that I can debug issues or reference the exact execution details.

---

## Scope

### In Scope
- Reusable CopyButton component (extract from TraceInfo)
- Enhanced TraceInfo with expand/collapse for details
- Display full response data: result, degradedReason, fallbackStrategy
- RawJsonViewer for formatted JSON display
- Copy JSON functionality
- Integration with existing result components

### Out of Scope
- Server-side DecisionLog API (not exposed to web)
- Full audit trail
- Export to file
- Share trace link

---

## Anchors (Non-negotiables)

| Artifact | Path | Constraint |
|----------|------|------------|
| TraceInfo | `components/commands/TraceInfo.tsx` | Enhance, not replace |
| CommandResponse | `types/api.ts` | Use existing types |
| History | `components/commands/CommandHistoryEntry.tsx` | Already has JSON view |

---

## Files to Change

| File | Action | Purpose |
|------|--------|---------|
| `clients/web/src/components/ui/CopyButton.tsx` | CREATE | Reusable copy button |
| `clients/web/src/components/commands/RawJsonViewer.tsx` | CREATE | Formatted JSON display |
| `clients/web/src/components/commands/TraceInfo.tsx` | MODIFY | Add expand/collapse, use CopyButton |
| `clients/web/src/components/commands/index.ts` | MODIFY | Export new components |
| `clients/web/src/styles/index.css` | MODIFY | Add trace viewer styles |

---

## Implementation Plan

### Step 1: Create CopyButton component

Reusable copy-to-clipboard button with feedback.

```typescript
interface CopyButtonProps {
  text: string;
  label?: string;
  successLabel?: string;
}
```

### Step 2: Create RawJsonViewer component

Collapsible JSON display with copy functionality.

```typescript
interface RawJsonViewerProps {
  data: unknown;
  label?: string;
}
```

### Step 3: Enhance TraceInfo

- Add `response` prop for full response data
- Add expand/collapse toggle
- Show expanded details: status, result, degraded info
- Use CopyButton for commandId and correlationId
- Add "Show Raw Response" toggle

### Step 4: Update result components

Pass full response to TraceInfo where needed.

### Step 5: Add CSS styles

Styles for expanded trace view and JSON viewer.

---

## Component Structure

```
TraceInfo (enhanced)
├── Collapsed: commandId, correlationId (copy), executionMs
├── Expanded:
│   ├── Status
│   ├── Initiator
│   ├── Result (if executed/degraded):
│   │   ├── Task ID
│   │   ├── Assignee
│   │   └── Confidence
│   ├── Degraded info (if degraded):
│   │   ├── Reason
│   │   └── Fallback Strategy
│   └── RawJsonViewer (toggle)
└── CopyButton (reused)
```

---

## Tests & Checks

### Verification Commands

```bash
cd clients/web && npm run build
cd clients/web && npm run lint
```

### Manual Testing
1. Submit command → verify trace section
2. Click expand → verify all details shown
3. Copy correlationId → verify clipboard
4. Show raw JSON → verify formatting
5. Copy JSON → verify clipboard

---

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Large JSON display | UI overflow | Max-height with scroll |
| Copy fails | No feedback | Error state handling |

---

## Done Criteria

| AC | DoD Check |
|----|-----------|
| AC1: Trace section | Always visible with basic info |
| AC2: Expanded view | Shows all response fields |
| AC3: Raw JSON | Formatted, copyable |
| AC4: Copy | Works for all copyable fields |
| AC5: From history | Already works (existing JSON view) |
| AC6: Minimal footprint | Collapsible, labeled "technical" |

---

## Anti-Scope-Creep

DO NOT:
- Add server-side DecisionLog API
- Add export functionality
- Add share link feature
- Over-complicate the UI
