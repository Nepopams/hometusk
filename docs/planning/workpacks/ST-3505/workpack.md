# Workpack: ST-3505 - Mobile Command Chat and Controlled Outcomes

## Sources of Truth

- Scope anchor: `docs/planning/initiatives/INIT-2026Q3-native-mobile-mvp.md`
- Execution index: `docs/planning/initiatives/INIT-2026Q3-native-mobile-mvp.execution.md`
- Epic: `docs/planning/epics/EP-035/epic.md`
- Story: `docs/planning/epics/EP-035/stories/ST-3505-mobile-command-chat.md`
- REST contract: `docs/contracts/http/commands.openapi.yaml`
- Mobile app: `clients/mobile/`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

## Status

**DONE / GATE D GO.** Gate B is delegated GO after ST-3504 Gate D. Artifact gate is GO with no contract change because `/commands` and `/commands/{commandId}/continue` already exist.

## Outcome

Mobile users can submit text commands through the HomeTusk command pipeline, see controlled outcomes, continue `needs_input`, and keep non-sensitive recent command history.

## Acceptance Criteria

- [x] AC-1: User can enter and submit a text command for the selected household.
- [x] AC-2: Mobile sends `source=mobile`, `Idempotency-Key`, and correlation ID.
- [x] AC-3: Executed, scheduled, needs_input, rejected, and degraded outcomes render as product states.
- [x] AC-4: `needs_input` can continue through the command continuation contract.
- [x] AC-5: Recent command history is stored only as non-sensitive local app memory.
- [x] AC-6: Mobile has no generic assistant behavior and no direct AI Platform calls.

## Non-goals

- Voice command.
- Streaming/wake word.
- Mobile-only AI prompts.
- Raw text-to-AI Platform calls.

## Implementation Plan

1. Add `continueCommand` to the mobile API client and type controlled command response fields.
2. Reuse existing command endpoint for submit; use a deterministic mobile text shell:
   - default text creates a task title;
   - `done <id-or-title>` completes a matched open task.
3. Render controlled command outcomes.
4. Add continuation input for `needs_input`.
5. Persist recent command hints through `localAppMemory.ts`.

## Verification Commands

- `cd clients/mobile && npm run typecheck`
- `cd clients/mobile && npx expo start --help`
- Source review for `/commands`, continuation, local command history, and no AI Platform calls.

## Evidence

- Mobile typecheck: `cd clients/mobile && npm run typecheck` passed.
- Expo CLI smoke: `cd clients/mobile && npx expo start --help` passed.
- Source review: `rg -n "continueCommand|commands/\\$\\{commandId\\}/continue|executeCommand|readRecentCommands|writeRecentCommands|RecentCommandHint|AI Platform|ai-platform|direct AI|Idempotency-Key|X-Correlation-ID|source: 'mobile'" clients/mobile/src clients/mobile/App.tsx clients/mobile/README.md clients/mobile/AGENTS.md` reviewed.
- No backend or contract changes were required for ST-3505.

## Risks

- The backend command contract is structured, not raw free-form assistant chat. Mitigation: mobile text shell is deterministic and still submits structured HomeTusk commands.
- Continuation input is generic key/value or clarification text; richer guided continuation can be improved later.

## Prompt Pack

- PLAN: `docs/planning/workpacks/ST-3505/prompt-plan.md`
- PLAN findings: `docs/planning/workpacks/ST-3505/plan-findings.md`
- Gate C: `docs/planning/workpacks/ST-3505/gate-c.md`
- APPLY: `docs/planning/workpacks/ST-3505/prompt-apply.md`
- Review gate: `docs/planning/workpacks/ST-3505/review-gate.md`
- Gate D: `docs/planning/workpacks/ST-3505/gate-d.md`
