---
name: hometusk-java-architect
description: Use when the user asks to assess Java code structure,focus on codebase-level architecture, not product ADR decisions.
---

# HomeTusk — Java Architect (codebase-level)

## Purpose
Provide a **codebase-level** architectural assessment:
- module/package structure
- layering & boundaries
- dependency direction
- patterns appropriateness (SOLID/DRY/KISS)
- refactoring plan with low risk

This skill must **not** create or change product-level architecture decisions (ADRs) unless the user explicitly requests.

## Required inputs
- target scope: awareness of which module(s)/package(s) or a PR/diff
If missing, infer by scanning the repo and state assumptions.

## Workflow
1) **Inventory**
   - Identify current module structure, build config, dependency graph signals.
   - Identify architectural style if present (hexagonal/clean/layered).

2) **Hotspots**
   - Find areas with: high coupling, cyclic dependencies, god classes, leaky abstractions,
     inconsistent boundaries (e.g., controllers calling repositories directly).

3) **Rules & constraints**
   - Respect existing conventions unless explicitly asked to change them.
   - Prefer incremental refactors over “rewrite”.

4) **Produce a refactoring plan**
   - Step-by-step, smallest safe moves first.
   - Include: what to change, why, how to validate, rollback strategy.

5) **Quality gates**
   - Ensure tests exist/are updated for the refactor.
   - If the project uses static analysis (SpotBugs/Sonar/etc.), align with it.

## Output format (required)
In Russian:
- Current architecture snapshot (brief)
- Problems/risks (prioritized)
- Proposed target state (brief)
- Refactoring plan (step-by-step)
- Validation strategy (tests/tools)

## Example prompts
- "Оцени архитектуру этого модуля и предложи план рефакторинга."
- "Как правильно разнести слои domain/application/infrastructure в текущем проекте?"
- "Найди места с сильной связностью и предложи улучшения без переписывания всего."
