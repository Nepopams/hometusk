---
name: hometusk-intake-triage
description: Use for any new HomeTusk request that needs scope classification, risk assessment, impact flags, human gates, or a next-step recommendation before planning or implementation.
---

# HomeTusk Intake Triage

## Purpose

Turn an incoming request into a scoped, artifact-backed next step without doing
implementation work.

## Inputs

Read enough context to identify:

- user goal and success criteria;
- active release or initiative scope;
- impacted area: docs, backend, web, contracts, data, security, observability;
- whether the request is PI, sprint, epic, story, bugfix, review, or cleanup.

## Workflow

1. Anchor to `AGENTS.md`, `docs/CODEX-WORKFLOW.md`, product goal, roadmap, and
   current scope anchor.
2. Classify:
   - change type: feature, bugfix, refactor, infra, contract-change, docs/process;
   - work level: PI, sprint, epic, story, workpack, review, small task.
3. Assess impact:
   - `contract_impact`;
   - `data_impact`;
   - `adr_needed`;
   - `diagrams_needed`;
   - `security_sensitive`;
   - `traceability_critical`.
4. Identify human gates:
   - contract or artifact gate before implementation;
   - Gate C before APPLY;
   - Gate D after review.
5. Recommend the next workflow skill or read-only custom agent.

## Allowed scope

- Read repo artifacts.
- Produce triage summaries, blockers, assumptions, and handoff recommendations.
- Recommend planning, contract, ADR/diagram, workpack, or review workflows.

## Forbidden scope

- Do not edit runtime code.
- Do not create contract, ADR, diagram, or workpack artifacts directly unless the
  user explicitly asks to proceed into that workflow.
- Do not invent scope that is not anchored to repo artifacts.
- Do not bypass human gates.

## Output

Respond in Russian with:

- request classification;
- source-of-truth files consulted;
- impact flags;
- required gates;
- next recommended workflow;
- blockers or assumptions.
