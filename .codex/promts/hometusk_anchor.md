AUTHORITATIVE CONTEXT (MUST READ before planning/changes)
- AGENTS.MD (project invariants, degraded mode, contracts governance)
- docs/contracts/http/commands.openapi.yaml (public HTTP contract source of truth)
- docs/architecture/service-catalog.md (modules/stages ownership)
- docs/mvp/api-coverage.md (MVP journey coverage matrix)
- docs/architecture/decisions/* (ADR series; update or add new ADR when needed)
- services/backend/src/main/java/... (only relevant packages for this step)
Hard rules:
- If you change runtime behavior or endpoints, you MUST update OpenAPI + service-catalog + api-coverage and mention exact files touched.
- Upstream AI contracts directory is read-only (do not modify).
- Output must include: (1) files consulted, (2) files changed, (3) verification commands.

SKILLS (use explicitly)
- Use hometusk-spring-boot-engineer for Spring Boot changes + integration tests.
- Use code-reviewer to sanity-check diffs (errors, boundary, contract drift).
- Use java-architect when making/adjusting ADR decisions.
