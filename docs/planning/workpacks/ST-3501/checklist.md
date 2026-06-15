# DoD Checklist: ST-3501 - Mobile Stack ADR and App Foundation

## Readiness

- [x] Active initiative identified from roadmap.
- [x] Gate A delegated GO recorded.
- [x] Initial Gate B delegated GO recorded for ST-3501.
- [x] Story has testable acceptance criteria.
- [x] Impact flags recorded.
- [x] Artifact gate identified.
- [x] PLAN prompt generated.
- [x] PLAN findings recorded.
- [x] Gate C delegated approval recorded after PLAN.

## Architecture / Docs

- [x] ADR-020 accepted.
- [x] ADR index updated.
- [x] Mobile push/deep-link diagram added.
- [x] Diagram index updated.
- [x] Draft mobile device contract added.
- [x] Contract index updated.
- [x] Service catalog updated.

## Mobile Foundation

- [x] `clients/mobile/package.json` exists.
- [x] `clients/mobile/app.json` exists.
- [x] `clients/mobile/README.md` exists.
- [x] `clients/mobile/AGENTS.md` exists.
- [x] Root app shell renders Home, Tasks, Shopping, and Command surfaces.
- [x] API config boundary exists.
- [x] Secure token storage boundary exists.
- [x] Non-sensitive local memory boundary exists.

## Guardrails

- [x] No Firebase/Supabase domain backend dependency.
- [x] No direct AI Platform dependency.
- [x] No PWA/Capacitor replacement.
- [x] Sensitive tokens are not stored in plain local storage.

## Verification

- [x] `cd clients/mobile && npm install` completed.
- [x] `cd clients/mobile && npm run typecheck` passes.
- [x] `cd clients/mobile && npx expo start --help` passes.

## Final

- [x] Workpack evidence updated after APPLY.
- [x] Review gate completed with GO before Gate D.
- [x] Gate D decision recorded.
