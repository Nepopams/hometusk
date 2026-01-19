# Story: Setup JDK/CI Environment

**ID:** ST-101
**Epic:** EP-002 (MVP Iteration 2)
**Iteration:** 2a
**Points:** 1
**Status:** ready
**Priority:** P0 (BLOCKER)

---

## Title

Setup JDK 21 and verify test execution

---

## Description

As a developer, I need JDK 21 configured so that I can run the test suite and verify MVP exit criteria.

**Context:**
Currently `JAVA_HOME is not set` error blocks all gradle commands. This must be resolved before any other work can proceed.

**User Value:**
Unblocks all testing and validation work.

---

## Acceptance Criteria

### AC1: JDK available
```
Given the development environment
When I run `java -version`
Then output shows Java 21.x
```

### AC2: Gradle works
```
Given JDK is configured
When I run `cd services/backend && ./gradlew --version`
Then gradle version is displayed without errors
```

### AC3: Tests execute
```
Given gradle works
When I run `./scripts/test.sh`
Then tests execute (pass or fail is determined)
And results are displayed
```

---

## Test Strategy

**Verification:**
- Manual: Run commands and verify output
- No new tests needed (this enables existing tests)

---

## Technical Notes

**Options:**
1. Install JDK 21 system-wide: `sudo apt install openjdk-21-jdk` (Ubuntu)
2. Use SDKMAN: `sdk install java 21-open`
3. Docker fallback: `docker run -v $(pwd):/app -w /app gradle:jdk21 ./gradlew test`

**Environment variables:**
```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
```

---

## Flags

| Flag | Value | Notes |
|------|-------|-------|
| contract_impact | no | Environment only |
| adr_needed | no | Standard setup |
| diagrams_needed | no | — |

---

## Definition of Ready Checklist

- [x] Title clear
- [x] AC testable
- [x] No code dependencies
- [x] Human can execute
