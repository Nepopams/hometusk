# Workpack: ST-101 — Setup JDK/CI Environment

**Story:** [ST-101-jdk-setup.md](../../epics/EP-002/stories/ST-101-jdk-setup.md)
**Type:** Human Runbook (no code changes)
**Priority:** P0 (BLOCKER)

---

## Sources of Truth

| Artifact | Path | Relevance |
|----------|------|-----------|
| Story spec | `docs/planning/epics/EP-002/stories/ST-101-jdk-setup.md` | AC definition |
| Gradle wrapper | `services/backend/gradlew` | Build tool |
| Gradle config | `services/backend/build.gradle.kts` | JDK version requirement |
| Test script | `scripts/test.sh` | Verification command |

---

## Prerequisites

- Linux system with apt or SDKMAN installed
- sudo access (for apt install) or user-level SDKMAN
- Internet connection for package download

---

## Implementation Plan

### Step 1: Verify current state

```bash
# Check if Java is installed
java -version 2>&1 || echo "Java not found"

# Check JAVA_HOME
echo "JAVA_HOME=$JAVA_HOME"
```

**Expected:** Error or Java version < 21

### Step 2: Install JDK 21

**Option A: Ubuntu/Debian (apt)**
```bash
sudo apt update
sudo apt install openjdk-21-jdk -y
```

**Option B: SDKMAN (cross-platform)**
```bash
# Install SDKMAN if not present
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# Install Java 21
sdk install java 21-open
```

**Option C: Docker fallback (if system install blocked)**
```bash
# Run tests via Docker (no system install needed)
docker run --rm -v $(pwd):/app -w /app/services/backend gradle:jdk21-alpine ./gradlew test
```

### Step 3: Configure environment

```bash
# For apt install (adjust path if needed)
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

# Persist in shell profile
echo 'export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64' >> ~/.bashrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc
source ~/.bashrc
```

### Step 4: Verify installation

```bash
java -version
# Expected: openjdk version "21.x.x" ...
```

---

## Verification Commands

| # | Command | Expected Result |
|---|---------|-----------------|
| 1 | `java -version` | `openjdk version "21.x.x"` |
| 2 | `echo $JAVA_HOME` | Valid path (e.g., `/usr/lib/jvm/java-21-openjdk-amd64`) |
| 3 | `cd services/backend && ./gradlew --version` | Gradle version displayed without errors |
| 4 | `./scripts/test.sh` | Tests execute (pass or fail is acceptable) |

---

## Files to Change

**None.** This is environment setup only.

---

## DoD Checklist

- [ ] `java -version` shows Java 21.x
- [ ] `./gradlew --version` succeeds
- [ ] `./scripts/test.sh` executes without JAVA_HOME error
- [ ] Environment variables persisted in shell profile

---

## Risks

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| No sudo access | Low | High | Use SDKMAN or Docker fallback |
| Package not available | Low | Medium | Use alternative package source or Docker |
| Path conflicts | Low | Low | Verify correct JAVA_HOME after install |

---

## Rollback

```bash
# If installed via apt
sudo apt remove openjdk-21-jdk

# If installed via SDKMAN
sdk uninstall java 21-open

# Remove from .bashrc
# (manually edit ~/.bashrc and remove JAVA_HOME lines)
```

---

## Contract Impact

**None.** Environment setup does not affect contracts.

---

## Docs Updates

**None required.** This enables existing documentation/tests to run.

---

## Notes

- This story unblocks ALL other stories in the sprint
- Once complete, immediately verify by running full test suite
- If any tests fail, document failures for triage (not a blocker for this story)
