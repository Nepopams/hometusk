# ST-SHOP-001-005 Review Gate

**Date:** 2026-06-13
**Decision:** GO

## Must-fix

None.

## Should-fix

None blocking initiative closure.

## Evidence

- Backend manual create-list, add/link, update/unlink, and cross-household rejection are covered by `ShoppingControllerTest`.
- Manual REST task links use household-scoped lookup and return `TASK_NOT_FOUND` for invalid or cross-household `linkedTaskId`.
- Command/AI add-shopping-item path keeps safe degradation for invalid task references.
- HTTP contract and implementation agree on `source` max length 120, nullable `linkedTaskId`, and explicit null unlink semantics.
- No upstream AI Platform snapshots were changed.

## Verification

- `cd clients/web && npm run build` — pass.
- `cd clients/web && npm test -- --run` — pass.
- `cd clients/web && npm run lint` — pass.
- `cd services/backend && ./gradlew compileJava compileTestJava --no-daemon` — pass.
- `cd services/backend && ./gradlew test --tests com.hometusk.integration.ShoppingControllerTest --no-daemon --info --stacktrace` — pass.
- `cd services/backend && ./gradlew test --info --no-daemon` — pass.
- `cd services/backend && ./gradlew spotlessCheck --no-daemon` — pass.
- `git diff --check` — pass; Git reports CRLF conversion warnings only.
- Browser smoke: web dev server opened at `http://127.0.0.1:5187/`, rendered dev-login, no console errors.

## Residual Risk

- Authenticated browser E2E for shopping/task screens was not run because this Windows session has no valid local JWT/backend auth setup available.
- `./scripts/test.sh` was not runnable because `/bin/bash`/WSL is unavailable in this session; covered by equivalent Gradle backend tests and web checks above.

## Gate D

Approved for initiative closure.
