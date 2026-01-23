# Checklist: ST-501 — Command Input Box

## Sources of Truth
- Workpack: `docs/planning/workpacks/ST-501/workpack.md`
- Story: `docs/planning/epics/EP-006/stories/ST-501-command-input-box.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Definition of Ready (DoR) Verification

- [x] Story has clear title and description
- [x] Acceptance criteria defined (Gherkin format)
- [x] In scope / out of scope explicit
- [x] Files to change listed
- [x] API dependencies documented (OpenAPI)
- [x] EP-005 complete (householdId available via useAuth())
- [x] EP-004 complete (Auth token provider available)
- [x] OpenAPI contract finalized for `POST /api/v1/commands`

**DoR Status: READY**

---

## Definition of Done (DoD) Checklist

### Code Quality
- [ ] Types added to `clients/web/src/types/api.ts`
- [ ] `executeCommand()` added to `clients/web/src/lib/api.ts`
- [ ] `useCommand.ts` hook created
- [ ] `CommandInput.tsx` component created
- [ ] `CreateTaskForm.tsx` component created
- [ ] `CompleteTaskForm.tsx` component created
- [ ] Barrel export `index.ts` created
- [ ] HouseholdLayout updated with CommandInput
- [ ] No lint errors: `npm run lint`
- [ ] No type errors: `npm run typecheck`
- [ ] Build passes: `npm run build`

### Functionality
- [ ] Form renders when authenticated + household selected
- [ ] Create task mode: title field required
- [ ] Create task mode: zone dropdown populated
- [ ] Create task mode: deadline input works
- [ ] Complete task mode: task dropdown shows open tasks
- [ ] Submit button triggers API call
- [ ] Enter key submits form
- [ ] Loading state shows during submission
- [ ] Success clears form / shows feedback
- [ ] 400 error shows validation message inline
- [ ] 401 error redirects to login
- [ ] 403 error shows "Access denied"
- [ ] 409 error shows "Command already submitted"

### Headers (per OpenAPI)
- [ ] `Idempotency-Key` header set (UUID format)
- [ ] `X-Correlation-ID` header set (UUID v4)
- [ ] `Authorization: Bearer <token>` header set
- [ ] `Content-Type: application/json` header set

### Request Body (per OpenAPI)
- [ ] `householdId` from `useAuth()` (AuthContext)
- [ ] `type` is enum: `create_task` | `complete_task`
- [ ] `payload` matches type-specific schema
- [ ] `source` is `'web'`

### Testing
- [ ] Manual test: create task happy path
- [ ] Manual test: complete task happy path
- [ ] Manual test: validation error display
- [ ] Manual test: loading state visible
- [ ] Manual test: keyboard submission (Enter)
- [ ] DevTools: verify request headers
- [ ] DevTools: verify request body structure

### Security
- [ ] householdId comes from authenticated context
- [ ] No sensitive data logged to console
- [ ] Auth token not exposed in error messages

---

## Acceptance Criteria Verification

| # | Criterion | Status | Evidence |
|---|-----------|--------|----------|
| AC1 | Form visible when authenticated + household selected | [ ] | Manual test |
| AC2 | Submit triggers POST /api/v1/commands | [ ] | DevTools network |
| AC3 | Request body matches CommandRequest schema | [ ] | DevTools payload |
| AC4 | Idempotency-Key header present (UUID) | [ ] | DevTools headers |
| AC5 | X-Correlation-ID header present (UUID v4) | [ ] | DevTools headers |
| AC6 | Authorization header present | [ ] | DevTools headers |
| AC7 | Loading state during submission | [ ] | Visual check |
| AC8 | 200 response clears loading | [ ] | Manual test |
| AC9 | 400 error shows inline message | [ ] | Mock or backend error |
| AC10 | 403 error shows "Access denied" | [ ] | Mock or backend error |
| AC11 | 409 error shows conflict message | [ ] | Mock or backend error |
| AC12 | 401 error redirects to login | [ ] | Manual test |

---

## Files Changed Verification

| File | Action | Verified |
|------|--------|----------|
| `clients/web/src/types/api.ts` | MODIFY | [ ] |
| `clients/web/src/lib/api.ts` | MODIFY | [ ] |
| `clients/web/src/hooks/useCommand.ts` | CREATE | [ ] |
| `clients/web/src/components/commands/CommandInput.tsx` | CREATE | [ ] |
| `clients/web/src/components/commands/CreateTaskForm.tsx` | CREATE | [ ] |
| `clients/web/src/components/commands/CompleteTaskForm.tsx` | CREATE | [ ] |
| `clients/web/src/components/commands/index.ts` | CREATE | [ ] |
| `clients/web/src/routes/HouseholdLayout.tsx` | MODIFY | [ ] |

---

## Verification Commands

```bash
# Run all checks
cd clients/web

# 1. Type check
npm run typecheck
# Expected: No errors

# 2. Lint
npm run lint
# Expected: No errors

# 3. Build
npm run build
# Expected: Build successful

# 4. Dev server
npm run dev
# Expected: Server starts, navigate to /households/:id
```

---

## Sign-off

| Role | Name | Date | Status |
|------|------|------|--------|
| Developer | | | [ ] Complete |
| Reviewer | | | [ ] Approved |
| QA (manual) | | | [ ] Verified |

---

## Notes
(To be filled during implementation)
