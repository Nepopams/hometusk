# Codex PLAN Prompt - ST-3504

Read-only PLAN for tasks and shopping mobile mutations.

## Scope

- Inspect existing command contract for create/complete task.
- Inspect existing shopping mutation endpoints.
- Confirm whether any contract/backend change is required.
- Identify mobile code changes and verification.

## Stop Conditions

- Stop if task create/complete would require direct task CRUD outside the command boundary.
- Stop if new backend contracts are required.
- Stop if mutations would need offline queue semantics.

## Expected Output

- Findings with paths.
- Approved APPLY scope.
- Risks and verification.
