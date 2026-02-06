# Codex PLAN: ST-1310 — URL Safe Encoding Guardrails

## Objective
Verify project structure and test setup before implementing URL encoding utility.

## Constraints
- **READ-ONLY** — no file modifications
- Allowed commands: `ls`, `cat`, `rg`, `grep`

## Questions to Answer

### Q1: Test framework setup
- Run `cat clients/web/package.json | grep -A5 '"vitest"'`
- Is vitest configured?
- What is the test command?

### Q2: Existing lib/ structure
- Run `ls clients/web/src/lib/`
- Are there other utility modules with tests?
- Naming conventions?

### Q3: Existing test patterns
- Run `ls clients/web/src/lib/*.test.ts` (if any)
- Or check `clients/web/src/__tests__/` structure

### Q4: TypeScript config
- Check if `.test.ts` files are included in tsconfig

## Expected Output

```
## Findings

### Test Framework
- vitest: [yes/no]
- test command: [command]

### lib/ structure
- files: [list]
- test files: [list]

### Test file location
- convention: [inline vs __tests__]

### Ready to implement
- [yes/no, any blockers]
```
