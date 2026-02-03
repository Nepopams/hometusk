# Sprint S16 — Scope Details

## In Scope

### EP-013: Shopping Marketplaces (Foundation)

#### ST-1301: ShoppingRun Entity + Repository (5 pts)
**Goal:** Create domain entity and JPA repository for shopping runs

**Deliverables:**
- `ShoppingRun` entity (id, householdId, listId, status, timestamps)
- `ShoppingRunItem` entity (snapshot of items)
- `ShoppingRunStatus` enum (ACTIVE, COMPLETED, CANCELLED)
- `ShoppingRunRepository` with household-scoped queries
- Database migration V025
- Unit tests for entity methods

**Acceptance Criteria:** See story spec

**Contract alignment:** `ShoppingRunDto`, `ShoppingRunItemDto` from OpenAPI

**ADR alignment:** ADR-014 (copy-on-snapshot, sync-back, terminal states)

---

#### ST-1303: Export Shopping List (3 pts)
**Goal:** Add export endpoint for text/CSV formats

**Deliverables:**
- `GET /households/{hid}/shopping-lists/{lid}/export?format=text|csv`
- Text format: human-readable list
- CSV format: RFC 4180 compliant with proper escaping
- Content-Type and Content-Disposition headers

**Acceptance Criteria:** See story spec

**Contract alignment:** Export endpoint from OpenAPI

---

#### ST-1304: Marketplace Link-out Templates (5 pts)
**Goal:** Serve marketplace URL templates from configuration

**Deliverables:**
- `GET /api/v1/marketplace-templates` endpoint
- `MarketplaceProperties` configuration class
- `MarketplaceConfigService` for enabled templates
- Configuration in `application.yml` (Ozon, Yandex Market)
- Startup validation (template format, {query} placeholder)

**Acceptance Criteria:** See story spec

**ADR alignment:** ADR-015 (config-based, {query} placeholder, client encodes)

---

#### ST-1309: Task-Shopping Navigation (3 pts)
**Goal:** Add navigation links between tasks and shopping items

**Deliverables:**
- TaskDetail page: "Related Shopping Items" section
- ShoppingDetail page: Show linked task on items
- Clickable links for navigation
- Empty states when no links

**Acceptance Criteria:** See story spec

**No new backend changes** — uses existing `linkedTaskId` field

---

### EP-012: Voice Input Polish

#### ST-1206: Error Handling UX (3 pts)
**Goal:** Improve error messaging for voice input failures

**Deliverables:**
- User-friendly error messages (permission denied, network, ASR failure)
- Clear recovery paths (retry, fallback to text)
- No technical jargon in UI

---

#### ST-1207: Client Telemetry Events (2 pts)
**Goal:** Add analytics events for voice input flow

**Deliverables:**
- Events: voice_start, voice_cancel, voice_upload_ok/fail, asr_ok/fail, command_submitted_from_voice
- Track edit rate (transcript modified before submit)
- Console logging (v0) with future analytics integration path

---

#### ST-1208: Cross-Browser + Accessibility (3 pts)
**Goal:** Ensure voice input works across browsers and is accessible

**Deliverables:**
- Test in Chrome, Firefox, Safari, Edge
- Graceful degradation where MediaRecorder not supported
- Keyboard navigation for voice controls
- ARIA labels and screen reader support

---

## Out of Scope (Explicit)

### Deferred to S17
- **ST-1302** ShoppingRun REST endpoints (8 pts) — blocked by ST-1301
- **ST-1305** Share/Export UI buttons — blocked by ST-1303
- **ST-1306** Marketplace link-out buttons — blocked by ST-1304

### Deferred to S18
- **ST-1307** ShoppingRun creation UI
- **ST-1308** ShoppingRun checklist UI

### Deferred to LATER
- Mobile browser support for voice input
- PWA/offline support
- Voice enhancements (streaming, wake word)

### Explicitly OUT
- Marketplace API integrations (OAuth, cart) — requires partnerships
- Per-household marketplace config — over-engineered for v0
- Price tracking/comparison

---

## Story Dependencies Within Sprint

```
ST-1301 (entity) ─┬─> [S17: ST-1302 endpoints]
                  │
ST-1303 (export) ─┴─> [S17: ST-1305 UI]

ST-1304 (templates) ──> [S17: ST-1306 UI]
                    └─> ST-1310 (stretch: encoding)

ST-1309 (navigation) ──> no blockers

ST-1206, ST-1207, ST-1208 ──> no blockers (parallel work)
```

---

## Readiness Checklist

| Story | DoR Met | Contract | ADR | Test Strategy |
|-------|---------|----------|-----|---------------|
| ST-1301 | ✅ | ✅ | ✅ ADR-014 | ✅ |
| ST-1303 | ✅ | ✅ | — | ✅ |
| ST-1304 | ✅ | ✅ | ✅ ADR-015 | ✅ |
| ST-1309 | ✅ | — | — | ✅ |
| ST-1206 | ✅ | — | — | ✅ |
| ST-1207 | ✅ | — | — | ✅ |
| ST-1208 | ✅ | — | — | ✅ |
