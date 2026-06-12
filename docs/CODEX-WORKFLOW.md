# HomeTusk Codex-only Delivery Workflow

HomeTusk now uses a Codex-only delivery pipeline. The active instruction chain is
`AGENTS.md`, scoped `AGENTS.md` files, `.agents/skills/**`, and read-only custom
agents in `.codex/agents/**`.

`CLAUDE.md` and `.claude/**` are legacy references kept for a later cleanup PR.

## Operating model

1. Intake and triage: classify the request, scope, risks, contract/data/NFR
   impacts, and required gates.
2. Planning: create or update PI, sprint, epic, or story artifacts only when the
   work needs that level of planning.
3. Artifact gate: handle contracts, ADRs, and diagrams before implementation
   when story flags require them.
4. Workpack: produce an implementation-ready packet with files, tests, risks,
   rollback, and DoD checks.
5. Codex PLAN: run read-only exploration and return a decision-complete plan.
6. Human Gate C: approve the PLAN before any APPLY.
7. Codex APPLY: implement only the approved scope with minimal diff.
8. Review gate: run a read-only GO/NO-GO review before Human Gate D.

## Human gates

- Gate A: PI scope/objectives/roadmap/risk posture.
- Gate B: sprint goal, committed scope, dependencies, and risks.
- Artifact gate: contract, ADR, or diagram changes before implementation.
- Gate C: Codex PLAN approval before APPLY.
- Gate D: merge, ship, rollback, or block after review evidence.

## Skills

Use `.agents/skills/**` for reusable workflow instructions. Skills are
instruction-only unless deterministic scripts become necessary later.

Core workflow skills:

- `hometusk-intake-triage`
- `hometusk-planning`
- `hometusk-contract-governance`
- `hometusk-adr-diagram-governance`
- `hometusk-workpack-prompts`
- `hometusk-review-gate`

Existing implementation-oriented project skills under `.codex/skills/**` remain
in place during the pilot to avoid duplicate trigger drift. Move them to
`.agents/skills/**` only after the Codex-only pilot is accepted.

## Custom agents

Use `.codex/agents/*.toml` only for read-heavy work:

- architecture review;
- contract drift review;
- ADR/diagram drift review;
- security review;
- observability review;
- test-gap review;
- planning audit;
- final review gate.

Do not use custom agents to write production code or mutate docs in parallel.
Artifact-producing workflows stay in the main Codex thread through skills.

## Sources of truth

Always anchor work to repository files:

- Product goal: `docs/planning/strategy/product-goal.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- Release scope: `docs/planning/releases/MVP.md`
- Governance: `docs/_governance/dor.md`, `docs/_governance/dod.md`
- Contracts: `docs/contracts/**`
- Upstream snapshots: `docs/integration/ai-platform/v1/upstream/**`
- ADRs: `docs/adr/**`, with legacy references in `docs/architecture/decisions/**`
- Diagrams: `docs/diagrams/**`, with legacy references in `docs/architecture/diagrams/**`
- Service catalog: `docs/architecture/service-catalog.md`

## Review gate

The review gate is read-only and produces:

```markdown
## Review Result: GO | NO-GO
### Must-fix
### Should-fix
### Evidence
### Commands
### Recommendation
```

Review must verify workpack acceptance criteria, DoD, contracts, ADRs, diagrams,
household boundaries, command traceability, and degraded-mode behavior when
relevant.
