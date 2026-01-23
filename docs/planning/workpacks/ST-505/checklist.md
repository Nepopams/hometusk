# Checklist: ST-505 — Minimal Trace Viewer

## Sources of Truth
- Workpack: `docs/planning/workpacks/ST-505/workpack.md`
- Story: `docs/planning/epics/EP-006/stories/ST-505-trace-viewer.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Definition of Ready (DoR) Verification

- [x] Story has clear title and description
- [x] Acceptance criteria defined (Gherkin format)
- [x] In scope / out of scope explicit
- [x] Files to change listed
- [x] ST-502 complete (TraceInfo exists)
- [x] Types exist in api.ts (CommandResponse)

**DoR Status: READY**

---

## Definition of Done (DoD) Checklist

### Code Quality
- [ ] CopyButton.tsx created (reusable)
- [ ] RawJsonViewer.tsx created
- [ ] TraceInfo.tsx enhanced with expand/collapse
- [ ] index.ts updated with exports
- [ ] index.css updated with styles
- [ ] No lint errors: `npm run lint`
- [ ] Build passes: `npm run build`

### Functionality

#### CopyButton
- [ ] Shows label (default "Copy")
- [ ] Copies text to clipboard
- [ ] Shows success state ("Copied")
- [ ] Handles copy errors gracefully
- [ ] Resets after timeout

#### RawJsonViewer
- [ ] Shows/hides JSON on toggle
- [ ] JSON is formatted (pretty-printed)
- [ ] Has copy JSON button
- [ ] Scrollable if large

#### TraceInfo (enhanced)
- [ ] Collapsed: commandId, correlationId, executionMs
- [ ] Expand/collapse toggle visible
- [ ] Expanded: status, initiatorId
- [ ] Expanded: result (taskId, assigneeId, confidence)
- [ ] Expanded: degraded info (if applicable)
- [ ] CopyButton for commandId
- [ ] CopyButton for correlationId
- [ ] RawJsonViewer toggle

---

## Acceptance Criteria Verification

| # | Criterion | Status | Evidence |
|---|-----------|--------|----------|
| AC1 | Trace section in result | [ ] | Manual test |
| AC2 | Expanded trace view | [ ] | Manual test |
| AC3 | Raw JSON view | [ ] | Manual test |
| AC4 | Copy functionality | [ ] | Manual test |
| AC5 | Trace from history | [ ] | Existing (JSON view) |
| AC6 | Minimal footprint | [ ] | Manual test |

---

## Files Changed Verification

| File | Action | Verified |
|------|--------|----------|
| `components/ui/CopyButton.tsx` | CREATE | [ ] |
| `components/commands/RawJsonViewer.tsx` | CREATE | [ ] |
| `components/commands/TraceInfo.tsx` | MODIFY | [ ] |
| `components/commands/index.ts` | MODIFY | [ ] |
| `styles/index.css` | MODIFY | [ ] |

---

## Verification Commands

```bash
cd clients/web

# 1. Lint
npm run lint
# Expected: No errors

# 2. Build
npm run build
# Expected: Build successful
```

---

## Sign-off

| Role | Name | Date | Status |
|------|------|------|--------|
| Developer | | | [ ] Complete |
| Reviewer | | | [ ] Approved |

---

## Notes
(To be filled during implementation)
