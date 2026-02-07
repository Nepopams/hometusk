# Sprint S18 — Scope Details

## In Scope

### EP-013: Shopping Marketplaces (Run UX — Final)

#### ST-1307: ShoppingRun Creation UI (5 pts)
**Goal:** Add UI to start a shopping trip from list page

**Deliverables:**
- "Start Shopping Trip" button on ShoppingDetail page
- Confirmation modal with list preview
- API call to `POST /households/{hid}/shopping-lists/{lid}/runs`
- Redirect to run checklist page after creation
- Loading state + error handling
- Disabled state for empty lists

**Acceptance Criteria:** See story spec (AC-1 through AC-5)

**UI Components:**
- StartTripButton (or inline in header)
- StartTripModal (confirmation dialog)

**Route:** After creation → redirect to `/households/{hid}/shopping-runs/{runId}`

---

#### ST-1308: ShoppingRun Checklist UI (8 pts)
**Goal:** Shopping trip checklist for marking items purchased

**Deliverables:**
- New route: `/households/{hid}/shopping-runs/{runId}`
- Item checklist with checkboxes
- Progress indicator (X of Y purchased)
- "Complete Trip" button → close run as COMPLETED
- "Cancel Trip" button → close run as CANCELLED
- Optimistic UI for checkbox updates
- Read-only view for closed runs
- Run summary after closure

**Acceptance Criteria:** See story spec (AC-1 through AC-7)

**API Calls:**
- `GET /households/{hid}/shopping-lists/{lid}/runs/{runId}` — fetch run
- `PATCH /households/{hid}/shopping-lists/{lid}/runs/{runId}/items/{itemId}` — update item
- `POST /households/{hid}/shopping-lists/{lid}/runs/{runId}/close` — close run

**UI Components:**
- ShoppingRunPage (new route)
- RunProgress (X of Y indicator)
- RunItemCheckbox (optimistic updates)
- CloseRunModal (confirmation for complete/cancel)
- RunSummary (post-closure view)

---

## Out of Scope (Explicit)

### Explicitly OUT for S18
- Multi-list runs (combining multiple lists)
- Scheduled/recurring runs
- Adding items during active run
- Item reordering in run
- Photos/receipts attachment
- Location-based auto-start

### Tech Debt (Carry to hygiene backlog)
- Pre-existing lint warnings in other files
- Frontend component test coverage

---

## Story Dependencies Within Sprint

```
ST-1307 (creation) ──> ST-1308 (checklist) uses created run

Both depend on:
├── ST-1302: REST endpoints (DONE in S17)
└── ShoppingDetail page (EXISTS)
```

**Note:** ST-1308 can be developed in parallel with ST-1307 since they share the same backend API. Just need to coordinate on route structure.

---

## New Routes

| Route | Component | Description |
|-------|-----------|-------------|
| `/households/:hid/shopping-runs/:runId` | ShoppingRunPage | Run checklist |

---

## Readiness Checklist

| Story | DoR Met | Contract | ADR | Test Strategy |
|-------|---------|----------|-----|---------------|
| ST-1307 | ✅ | ✅ (ST-1302) | ADR-014 | ✅ |
| ST-1308 | ✅ | ✅ (ST-1302) | ADR-014 | ✅ |

---

## Demo Scenarios

1. **Create Run:**
   - Open shopping list with items
   - Click "Start Shopping Trip"
   - Confirm in modal
   - Verify redirect to run page

2. **Mark Items:**
   - Check off 3 of 5 items
   - Verify progress updates
   - Uncheck one item, verify decrement

3. **Complete Run:**
   - Click "Complete Trip"
   - Confirm with summary
   - Verify read-only view

4. **Cancel Run:**
   - Start new run
   - Click "Cancel Trip"
   - Verify redirect to list

5. **Error Recovery:**
   - Simulate network error on checkbox
   - Verify rollback + snackbar
