# Sprint S17 — Scope Details

## In Scope

### EP-013: Shopping Marketplaces (UI Layer)

#### ST-1302: ShoppingRun REST Endpoints (8 pts)
**Goal:** Full CRUD API for shopping runs

**Deliverables:**
- `POST /households/{hid}/shopping-lists/{lid}/runs` — create run (snapshot items)
- `GET /households/{hid}/shopping-lists/{lid}/runs` — list runs
- `GET /households/{hid}/shopping-lists/{lid}/runs/{rid}` — get run details
- `POST /households/{hid}/shopping-lists/{lid}/runs/{rid}/close` — close run
- `PATCH /households/{hid}/shopping-lists/{lid}/runs/{rid}/items/{iid}` — update item status

**Implementation:**
- 7 DTOs: CreateRequest, CloseRequest, UpdateItemRequest, ItemCountsDto, RunItemDto, RunDto, SummaryDto
- ShoppingRunService with business logic
- ShoppingRunController with 5 endpoints
- Integration tests covering happy path + edge cases

**Contract alignment:** `shopping-runs.openapi.yaml`

---

#### ST-1305: Share/Export UI Buttons (3 pts)
**Goal:** Add Share and Export CSV buttons to ShoppingDetail header

**Deliverables:**
- `exportShoppingList()` function in `api.ts`
- Share button: copies text format to clipboard
- Export button: downloads CSV file
- Snackbar feedback for success/error

**Implementation:**
- Inline SVG icons (share/download)
- Blob + URL.createObjectURL for CSV download
- navigator.clipboard.writeText for share

---

#### ST-1306: Marketplace Link-out Buttons (3 pts)
**Goal:** Show marketplace icons on shopping items for quick search

**Deliverables:**
- `getMarketplaceTemplates()` function in `api.ts`
- `useMarketplaceTemplates` hook
- Marketplace icons on each unpurchased item
- Uses `buildMarketplaceUrl()` from ST-1310 for safe encoding

**Implementation:**
- Icons from template iconUrl or fallback SVG
- Opens in new tab with noopener/noreferrer
- stopPropagation to prevent row click interference
- Hidden on purchased items (cleaner UI)

---

## Out of Scope (Explicit)

### Deferred to S18
- **ST-1307** ShoppingRun creation UI (start run button + modal)
- **ST-1308** ShoppingRun checklist UI (run-specific view)

### Explicitly OUT
- In-app browser for marketplace
- Price display/comparison
- Preferred marketplace storage per user
- Deep links to specific products (just search)

---

## Story Dependencies

```
S16 Foundation:
  ST-1301 (entity) ──┐
  ST-1303 (export) ──┼──> S17 UI
  ST-1304 (templates) ─┘
  ST-1310 (encoding) ──> ST-1306 uses buildMarketplaceUrl()

S17 UI (no internal deps):
  ST-1302 (endpoints) ──> independent
  ST-1305 (share/export) ──> uses ST-1303 endpoint
  ST-1306 (marketplace) ──> uses ST-1304 endpoint + ST-1310 utility
```

---

## Readiness Checklist

| Story | DoR Met | Contract | ADR | Test Strategy | Completed |
|-------|---------|----------|-----|---------------|-----------|
| ST-1302 | ✅ | ✅ | ADR-014 | ✅ | ✅ |
| ST-1305 | ✅ | ✅ | — | ✅ | ✅ |
| ST-1306 | ✅ | ✅ | ADR-015 | ✅ | ✅ |
