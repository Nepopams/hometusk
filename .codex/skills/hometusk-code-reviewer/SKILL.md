---
name: hometusk-code-reviewer
description: Use when the user asks for a code review of a PR/branch/diff/patch
---

# HomeTusk — Code Reviewer

## Purpose
Perform a systematic code review with emphasis on:
- correctness & edge cases
- security & data safety
- performance & resource usage
- maintainability & readability
- test quality & coverage signals (when available)

This skill is for **reviewing changes** (diff/PR/commit range), not for making architectural decisions.

## Required inputs
Ask for **one** of the following if not already provided:
- PR link/number, or
- branch name + base branch, or
- commit range (e.g., `main..feature/x`), or
- patch/diff text.

If still missing, proceed with the available context and explicitly state what you could not verify.

## Workflow
1) **Scope & context**
   - Identify language/frameworks touched.
   - Identify user-facing behavior changes, data model changes, and security-sensitive areas.
   - Identify any referenced specs/ADRs/contracts; if mentioned, open them.

2) **Read the diff systematically**
   - Start with high-level intent: what problem is being solved?
   - Then review file-by-file; prioritize:
     - auth/authz boundaries
     - input validation
     - persistence layer (migrations/queries)
     - error handling / retries / timeouts
     - external calls / integration points
     - logging (avoid secrets/PII)

3) **Security pass (must-not-miss)**
   - Injection risks (SQL, JSON, template, command)
   - Auth checks present and correct
   - Sensitive data not logged
   - Dependency/config changes that increase exposure

4) **Correctness & maintainability pass**
   - invariants enforced in code (not in assumptions)
   - null/empty handling, time zones, boundaries, concurrency
   - naming, cohesion, complexity hotspots, duplication
   - API compatibility and backward-compat expectations if endpoints/contracts changed

5) **Tests & verification**
   - Look for tests that cover new/changed behavior and edge cases.
   - If repo has test commands documented, run them and report results.
   - If you cannot run tests, say why and propose the minimal set to run in CI/local.

## Output format (required)
Provide review in Russian with these sections:

### Summary
- 2–5 bullets: what changed, main risk areas.

### Must fix
- Security/correctness issues that can lead to bugs, data loss, or vulnerabilities.

### Should fix
- Maintainability, readability, performance improvements.

### Nice to have
- Optional improvements, refactors, polish.

### Tests
- What you ran (exact commands) + result
- Or what should be run and why (if you couldn't run them)

### Questions (only if blocking)
- Ask at most 1–2 concise questions if necessary to approve.

## Example prompts
- "Сделай ревью этого PR, найди критические проблемы и предложи исправления."
- "Проанализируй diff и скажи, что может сломаться в проде."
- "Проверь изменения на security и корректность, особенно валидацию входных данных."
