# Epic: EP-006 — Command Box UI + Status Display

## Sources of Truth
- Initiative: `docs/planning/initiatives/INIT-2026Q2-command-ux.md`
- Product Goal: `docs/planning/strategy/product-goal.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`
- OpenAPI: `docs/contracts/http/commands.openapi.yaml`

---

## Status
**Ready** — All stories pass DoR, no blockers (2026-01-23)

## Initiative Alignment
This epic implements the **NOW** increment of INIT-2026Q2-command-ux:
- Command input box
- Call `POST /api/v1/commands` with proper headers
- UI statuses: executed / needs_input / rejected / executed_degraded
- Display "why" (errorCode/reason) and "what to do next"
- Command history (last N)
- Minimal trace viewer: correlationId + decision summary

---

## Epic Goal
Enable a user to:
1. Enter a natural language command in a text input
2. Submit it to the backend via `POST /api/v1/commands`
3. See the result status (executed, needs_input, rejected, executed_degraded)
4. Understand why (reason/errorCode) and what to do next
5. View recent command history per household
6. Access trace info (correlationId + decision summary)

This validates the core NL-first product hypothesis end-to-end in web.

---

## In Scope

### Command Input Box (ST-501)
- Text input component for command entry
- Submit button + keyboard shortcut (Enter)
- `POST /api/v1/commands` integration
- Generate `Idempotency-Key` (client-side UUID)
- Generate/propagate `X-Correlation-ID`
- Loading state during submission
- Basic success/error feedback

### Command Status Display (ST-502)
- Display all 4 response statuses with distinct UI:
  - `executed` - success with result summary
  - `needs_input` - prompt for clarification
  - `rejected` - show errorCode + reason
  - `executed_degraded` - success with warning badge
- "What to do next" hints for each status
- Action buttons where appropriate (retry, edit, etc.)

### Command History (ST-503)
- Persist last N commands per household (localStorage)
- Display recent commands list
- Show: input text, status badge, timestamp
- Click to view details/trace

### needs_input Basic Display (ST-504)
- Show `question` field from response
- Show `requiredFields` list
- Show `suggestions` if present
- Clear CTA: "Please retype your command with more details"
- NOTE: Full continuation UX (form + POST /continue) is NEXT scope

### Minimal Trace Viewer (ST-505)
- Display `correlationId` prominently
- Show `executionMs`
- Show `result` object (taskId, assigneeId, decisionConfidence)
- Show `degradedReason` and `fallbackStrategy` if present
- Link/expand to raw JSON (dev-friendly, not "debug dump")

---

## Out of Scope (explicit)

### Deferred to NEXT
- **Full needs_input UX** (form for additional input, `POST /commands/{id}/continue`)
- **Batch commands** (multiple intents in one input)
- **Rich suggestions/autocomplete**
- **Voice input**

### Never in Scope
- Backend changes to /commands API
- New intents (create_task, complete_task are already supported)
- Complex task editors
- ML/AI improvements

---

## Security & Data Boundaries

### Household Scoping
- Commands are always scoped to selected household
- `householdId` from current HouseholdContext
- Backend enforces membership (403 if not member)

### Idempotency
- Client generates unique `Idempotency-Key` per submission
- Same key + same payload = replay safe
- Same key + different payload = 409 IDEMPOTENCY_CONFLICT

### No Cross-Household Leaks
- Command history stored per-household in localStorage
- Commands only visible within their household context

---

## API Dependencies (from OpenAPI)

| Endpoint | Method | Used By |
|----------|--------|---------|
| `POST /api/v1/commands` | POST | ST-501, ST-502 |
| (response schemas) | — | ST-502, ST-504, ST-505 |

**Contract gaps:** None. All required schemas exist:
- `CommandRequest`
- `CommandResponse` (discriminated union: executed/needs_input/rejected/executed_degraded)
- `CommandErrorResponse`

---

## Stories

| ID | Title | Status | Priority | Points |
|----|-------|--------|----------|--------|
| ST-501 | Command Input Box | Ready | P1 | 3 |
| ST-502 | Command Status Display | Ready | P1 | 3 |
| ST-503 | Command History | Ready | P2 | 2 |
| ST-504 | needs_input Basic Display | Ready | P1 | 2 |
| ST-505 | Minimal Trace Viewer | Ready | P2 | 2 |

**Total:** 12 points

### Sprint Mapping
- **Sprint S05 (committed):** ST-501, ST-502, ST-504 (core flow, 8 points)
- **Sprint S05 (stretch):** ST-503, ST-505 (enhancements, 4 points)

---

## Dependencies

| Dependency | Type | Status | Notes |
|------------|------|--------|-------|
| EP-003 (Web Foundation) | Internal | Done | React app, routing |
| EP-004 (Auth/Session) | Internal | Done | Token handling |
| EP-005 (Household Lifecycle) | Internal | Done | Household context |
| Backend /commands API | Internal | Ready | OpenAPI complete |

---

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| needs_input UX confusion | Users don't know what to do | ST-504 shows clear hints; full form in NEXT |
| Idempotency-Key collisions | Duplicate commands | UUID v4 generation (collision-free) |
| History localStorage bloat | Browser storage limits | Cap at N items (e.g., 50), prune old |
| Degraded mode unclear | Users think it failed | ST-502 shows "completed with limitations" badge |

---

## Exit Criteria (NOW delivered)

From initiative INIT-2026Q2-command-ux:

1. User can type command in web and submit
2. User sees result status (all 4 types rendered correctly)
3. User understands what happened (trace info visible)
4. needs_input shows what's needed (even if continuation is NEXT)
5. Command history visible per household
6. 100% commands have trace (correlationId)
7. No cross-household leaks

---

## Flags

| Flag | Value | Notes |
|------|-------|-------|
| contract_impact | no | Consuming existing API |
| adr_needed | no | Standard UI patterns |
| diagrams_needed | no | No structural changes |
| security_sensitive | yes | Command execution, household scoping |

---

## Related Artifacts

| Artifact | Path |
|----------|------|
| Initiative | `docs/planning/initiatives/INIT-2026Q2-command-ux.md` |
| OpenAPI | `docs/contracts/http/commands.openapi.yaml` |
