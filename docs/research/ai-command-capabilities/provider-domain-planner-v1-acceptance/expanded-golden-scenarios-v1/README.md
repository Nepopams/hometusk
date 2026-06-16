# Expanded Golden Scenarios v1

Status: HomeTusk product acceptance asset, 2026-06-15

Parent initiative:
`docs/planning/initiatives/INIT-2026Q3-ai-provider-domain-planner-v1-acceptance-review.md`

## Purpose

This fixture package expands the 10 seed Domain Planner v1 scenarios to 50
HomeTusk-owned product scenarios.

It is an acceptance asset for future provider evaluation. It does not approve
HomeTusk runtime, OpenAPI, backend, mobile, AI Platform schema, or production
rollout changes.

## Files

- `context-fixtures-v1.yaml` - stable household contexts and ambiguity fixtures.
- `golden-scenarios-v1.yaml` - 50 product-owned scenario expectations.

## Coverage

The suite covers:

- simple task creation variations;
- multi-item shopping;
- shopping quantities and units;
- shopping list/source ambiguity;
- Russian colloquial phrasing;
- ASR-like noisy transcript variants;
- ambiguous member/list/task references;
- non-requester assignment;
- task plus shopping linkage;
- reschedule requests;
- completion requests;
- status/query commands;
- unsafe batch assignment;
- cross-household / unverifiable references;
- unsupported commands.

## Eval Rules

- Provider output must be schema-valid.
- Unsupported actions must not execute.
- Cross-household or unverifiable references must not execute.
- Ambiguity must clarify or use non-executing confirmation where supported.
- Confidence is not execution permission.
- HomeTusk remains validation, guardrail, execution, and audit authority.
- `confirm` and `answer` scenarios remain non-executing until contracts exist.

## Provider Use

Future provider eval should emit per-scenario rows with:

- scenario id;
- schema validity;
- expected/actual outcome;
- expected/actual intent;
- action types;
- unsupported auto-execute flag;
- cross-household reference flag;
- forbidden assumption flag;
- trace completeness;
- failure buckets.
