# Codex APPLY Prompt: ST-3401 - Minimal Household Dashboard Home

## Objective
Implement the minimal household dashboard home approved by Gate C.

## Allowed Files
- `clients/web/src/routes/Dashboard.tsx`
- `clients/web/src/routes/Dashboard.css`
- `clients/web/src/components/Layout/Sidebar.tsx`
- `clients/web/src/i18n/translations.ts`
- `docs/planning/epics/EP-034/epic.md`
- `docs/planning/epics/EP-034/stories/ST-3401-household-dashboard-minimal.md`
- `docs/planning/workpacks/ST-3401/workpack.md`
- `docs/planning/workpacks/ST-3401/checklist.md`
- `docs/planning/workpacks/ST-3401/review-gate.md`
- `docs/planning/workpacks/ST-3401/gate-d.md`

## Forbidden Files
- `docs/integration/ai-platform/v1/upstream/**`
- `docs/contracts/**`
- `docs/adr/**`
- `docs/diagrams/**`
- `services/backend/**`
- Backend migrations or service catalog files

## Invariants
- HomeTusk remains intent-driven; no command pipeline behavior changes.
- AI remains external; no local LLM behavior.
- Dashboard data must stay scoped to the selected household.
- `ProtectedRoute requireHousehold` remains in place.
- Invalid or unauthorized API responses must not be silently hidden.

## Acceptance Criteria
- Household navigation exposes Home/Dashboard.
- `/households/{householdId}` shows tasks, shopping lists, routines, and members cards.
- Cards use existing household-scoped hooks.
- Empty states and CTAs route to existing flows.
- Desktop and mobile layouts are readable without overlap.

## Verification
- `npm run build` in `clients/web`
- `npm run lint` in `clients/web`
- Browser desktop and mobile checks

## STOP THE LINE
Stop and report if:
- A new backend endpoint becomes required.
- A contract or schema change becomes necessary.
- Existing household hooks are insufficient for AC-2.
- Runtime changes need files outside the allowed list.
