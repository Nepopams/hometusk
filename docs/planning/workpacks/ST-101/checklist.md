# ST-101 Verification Checklist

## Acceptance Criteria

- [ ] **AC1:** `java -version` shows Java 21.x
- [ ] **AC2:** `./gradlew --version` runs without errors
- [ ] **AC3:** `./scripts/test.sh` executes (results captured)

## DoD Items

- [ ] Environment variables set (`JAVA_HOME`, `PATH`)
- [ ] Configuration persisted (shell profile)
- [ ] Verification commands all pass

## Sign-off

| Criterion | Status | Evidence |
|-----------|--------|----------|
| JDK installed | | `java -version` output |
| Gradle works | | `./gradlew --version` output |
| Tests run | | `./scripts/test.sh` output (summary) |

---

**Completed by:** _______________
**Date:** _______________
