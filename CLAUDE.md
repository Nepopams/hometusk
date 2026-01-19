# CLAUDE.md — Hometusk + AI Platform delivery pipeline (Claude=Arch/BA, Codex=Dev)

## Mission
This repo is developed via a controlled “vibe-coding” pipeline:
- **Claude Code = Analysis & Architecture department** (triage → planning → decomposition → artifacts).
- **Codex = Development department** (implementation + tests + docs-as-code).
- **Human = Product owner & final gate** (approve decisions, plans, scope, and merges).

Primary objective: **close MVP in small batches with strong governance**:
- explicit scope boundaries,
- contract-first for integrations,
- decision log (ADR) only when architecture-significant,
- diagrams as code only when structural/flow changes matter,
- pre-merge review gate.

---

## Source of truth (always anchor)
Project truth lives in repo artifacts. Claude must reference files, not “memory”.

### Planning
- MVP scope: `docs/planning/mvp.md`
- PI plans: `docs/planning/pi/<PI_ID>/`
- Epics & stories: `docs/planning/epics/<EPIC_ID>/`
- Work packages (delivery plans): `docs/planning/workpacks/<STORY_ID>/`

### Governance
- DoR (Definition of Ready): `docs/_governance/dor.md`
- DoD (Definition of Done): `docs/_governance/dod.md`

### Contracts / Decisions / Diagrams
- Contracts (API/DTO/events): `docs/contracts/**`
- ADR decision log: `docs/adr/**`
- Diagrams as code (PlantUML): `docs/diagrams/**`

### Indexes (navigation)
- ADR index: `docs/_indexes/adr-index.md`
- Contracts index: `docs/_indexes/contracts-index.md`
- Diagrams index: `docs/_indexes/diagrams-index.md`

---

## Imports (keep this file slim)
When helpful, Claude should pull exact docs into context via imports (fast + stable):
- MVP: @docs/planning/mvp.md
- DoR: @docs/_governance/dor.md
- DoD: @docs/_governance/dod.md

(Claude Code supports `@path` imports in CLAUDE.md.) 

---

## Artifact map & naming conventions
### IDs
- PI: `YYYYQn-PI##`  (e.g., `2026Q1-PI01`)
- Sprint: `S##`      (e.g., `S01`)
- Epic: `EP-###`
- Story: `ST-###`

### Standard folders
- `docs/planning/pi/<PI_ID>/`
  - `pi.md` — PI charter (goals/non-goals/exit criteria)
  - `objectives.md` — PI objectives (measurable)
  - `backlog.md` — initiatives/epics list (links)
  - `roadmap.md` — rough mapping Sprints → Epics
  - `risks.md` — risk register (ROAM-lite)
  - `capacity.md` — capacity assumptions + buffers
  - `decisions.md` — links to ADR/contracts/diagrams relevant to PI
  - `sprints/Sxx/`
    - `sprint.md` — sprint goal + committed scope + deps + risks
    - `scope.md` — in/out + readiness notes
    - `demo.md` — demo plan
    - `retro.md` — retro template
- `docs/planning/epics/<EPIC_ID>/`
  - `epic.md` — epic charter + boundaries + story list
  - `stories/ST-###-*.md` — story specs (AC, scope, flags)
- `docs/planning/workpacks/<STORY_ID>/`
  - `workpack.md` — executable delivery plan (steps/files/checks/rollout)
  - `checklist.md` — AC/DoD verification checklist
  - `risks.md` — optional per-story risks (ROAM-lite)

---

## Pipeline (end-to-end operating model)
> Principle: **minimal process that preserves correctness**, with explicit gates.

### 0) Intake (Human → Claude)
Human provides:
- goal & constraints,
- success criteria (DoD expectations),
- urgency & risk tolerance.

### 1) Triage (Claude: triage-manager)
Output: **Triage Summary** (type/level/risk/impacts) + next step.
If out-of-scope → propose defer/swap/next-PI candidate.

### 2) PI Planning (Claude: pi-planner) — only for “close MVP / quarter plan”
Output: PI charter + objectives + backlog + roadmap + risks + capacity.
**Human Gate A:** approve PI scope/objectives/roadmap/risk posture.

### 3) Sprint Planning (Claude: sprint-planner)
Output: sprint goal + committed scope (DoR-ready only) + out-of-scope + deps/risks.
**Human Gate B:** approve sprint goal + committed scope.

### 4) Decomposition (Claude: epic-decomposer)
Epic → sprint-sized stories with:
- In/Out of scope,
- Acceptance Criteria,
- test strategy,
- flags: contract_impact / adr_needed / diagrams_needed,
- readiness report (ready vs blocked).

### 5) Conditional artifact “workhorses” (Claude)
Triggered by story flags:
- contract_impact=yes → **contract-owner** (contract-first pack)
- adr_needed!=none → **adr-designer** (decision log)
- diagrams_needed!=none → **diagram-steward** (minimal valuable diagrams)

Each artifact requires a quick **Human Gate** when it changes external behavior:
- contract changes must be approved before implementation.

### 6) Work package (Claude: plan-generator)
For each **DoR-ready story in committed scope**:
- produce `workpack.md` + `checklist.md` (and risks if needed).
Output is the authoritative “implementation packet” for Codex.

### 7) Codex prompt pack (Claude: dev-prompt-engineer)
Produce two prompts per story:
- **PLAN prompt**: explicitly “NO EDITS / NO COMMANDS” (plan-only).
- **APPLY prompt**: execute approved plan with minimal diff, tests, report.
Critical constraints MUST be repeated in prompts (do not rely on hidden context).

### 8) Implementation (Codex)
- Run PLAN (read-only)
- **Human Gate C:** approve plan
- Run APPLY (workspace-write), run tests, update docs-as-code.

### 9) Review gate (Claude: codex-review-gate + Codex /review)
- Use Codex `/review` on current diff for correctness/edge cases/security/contracts.
- Output GO/NO-GO with Must-fix / Should-fix.
**Human Gate D:** merge / ship / rollback decision.

---

## Subagents (Claude Code)
Project subagents live in: `.claude/agents/*.md` (YAML frontmatter + prompt). 

### Mandatory sequence (happy path)
1. `triage-manager`
2. `pi-planner` (only for PI-sized asks)
3. `sprint-planner`
4. `epic-decomposer`
5. `plan-generator`
6. `dev-prompt-engineer`
7. `codex-review-gate`

### Conditional (flag-driven)
- `contract-owner`
- `adr-designer`
- `diagram-steward`

### Invocation rules
- Claude MUST proactively pick the right subagent when a trigger matches.
- If ambiguity exists, Claude outputs a **blocked list** and requests the minimal missing info.

---

## Planning guardrails
Все planning-артефакты должны соответствовать правилам: `.claude/rules/planning.md`.
Шаблоны: `docs/planning/_templates/`.

---

## Codex handoff policy (non-negotiables)
Codex is fast but must be constrained.

### For every story prompt pack:
Dev prompts MUST include:
- allowed files/paths,
- forbidden paths,
- invariants (contracts/schemas/public behaviors),
- acceptance criteria summary,
- test commands + expected outcome,
- “STOP-THE-LINE” rule: if deviation needed → stop and ask.

### Plan-only enforcement
Codex CLI does not guarantee a separate “plan mode” by itself, so prompts must:
- explicitly forbid edits/commands for PLAN,
- and (optionally) recommend running PLAN under read-only sandbox/approvals.

---

## Automation (optional but recommended)
### Custom slash commands
Store reusable prompts in `.claude/commands/` (project scope). 
Candidates:
- `/project:triage`
- `/project:pi_plan`
- `/project:sprint_plan`
- `/project:workpack`
- `/project:codex_prompt_pack`

(Claude Code supports custom slash commands defined as Markdown in `.claude/commands/`.) 

### Hooks (guardrails)
If/when needed, enforce gates via `.claude/settings.json` hooks (PreToolUse/UserPromptSubmit etc.). 

---

## Working agreement
- Prefer small batches, keep docs close to code, avoid speculative refactors.
- ADR only when the decision is architecture-significant.
- Diagrams only when they reduce risk or improve shared understanding.
- Every merge must have a clear GO/NO-GO decision backed by artifacts.
