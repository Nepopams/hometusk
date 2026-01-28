# Codex APPLY Prompt: ST-906 — Privacy Settings + Opt-out

## Mode
**APPLY** — implement the approved plan.

## Sources of Truth
- Workpack: `docs/planning/workpacks/ST-906/workpack.md`
- Story: `docs/planning/epics/EP-009/stories/ST-906-privacy-settings.md`
- DoD: `docs/_governance/dod.md`

---

## PLAN Clarifications (Human Gate Approved)

1. **Migration version: V019** (latest is V018)
2. **Separate GET /settings endpoint** — minimal API change, no breaking existing /progress contract
3. **reverseForTaskUncompleted stays enabled** — even when gamificationEnabled=false (to avoid leaving old points behind)
4. **Lazy-create on GET /settings** — settings created with defaults on first access
5. **Entity uses @ManyToOne** for User and Household (following project patterns)

### Actual Method Signatures (from PLAN discovery)
- `PointsService.awardForTaskCompleted(Task task, User actor)` — returns `List<PointsLedger>`
- `PointsService.reverseForTaskUncompleted(Task task, User actor)` — keep enabled regardless of settings
- `BadgeService.checkAndAwardBadges(User user, Household household)` — returns void

---

## Critical Constraints

### Settings Behavior
- Default: `showProgressToOthers = true`, `gamificationEnabled = true`
- When `gamificationEnabled = false`: NO points, NO badges (but reverseForTaskUncompleted still runs)
- When `showProgressToOthers = false`: user hidden from breakdown (future), but points count in aggregate
- Settings are per-user per-household
- Lazy-create settings with defaults on first GET /settings access

### Non-Toxic Copy
| Element | Text |
|---------|------|
| Privacy section title | "Privacy Settings" |
| Show progress toggle | "Show my progress to household members" |
| Enable gamification toggle | "Enable gamification" |
| Disable warning | "You won't earn points or badges while gamification is disabled." |

---

## Task 1: Backend — Entity

### File: `services/backend/src/main/java/com/hometusk/gamification/domain/GamificationSettings.java`
```java
package com.hometusk.gamification.domain;

import com.hometusk.households.domain.Household;
import com.hometusk.users.domain.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "gamification_settings",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "household_id"}))
public class GamificationSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;

    @Column(name = "show_progress_to_others", nullable = false)
    private boolean showProgressToOthers = true;

    @Column(name = "gamification_enabled", nullable = false)
    private boolean gamificationEnabled = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected GamificationSettings() {}

    public GamificationSettings(User user, Household household) {
        this.user = user;
        this.household = household;
        this.showProgressToOthers = true;
        this.gamificationEnabled = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters
    public UUID getId() { return id; }
    public User getUser() { return user; }
    public Household getHousehold() { return household; }
    public boolean isShowProgressToOthers() { return showProgressToOthers; }
    public boolean isGamificationEnabled() { return gamificationEnabled; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // Setters
    public void setShowProgressToOthers(boolean showProgressToOthers) {
        this.showProgressToOthers = showProgressToOthers;
    }

    public void setGamificationEnabled(boolean gamificationEnabled) {
        this.gamificationEnabled = gamificationEnabled;
    }
}
```

---

## Task 2: Backend — Repository

### File: `services/backend/src/main/java/com/hometusk/gamification/repository/GamificationSettingsRepository.java`
```java
package com.hometusk.gamification.repository;

import com.hometusk.gamification.domain.GamificationSettings;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GamificationSettingsRepository extends JpaRepository<GamificationSettings, UUID> {
    Optional<GamificationSettings> findByUser_IdAndHousehold_Id(UUID userId, UUID householdId);
}
```

---

## Task 3: Backend — DTO

### File: `services/backend/src/main/java/com/hometusk/gamification/dto/GamificationSettingsDto.java`
```java
package com.hometusk.gamification.dto;

import com.hometusk.gamification.domain.GamificationSettings;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Gamification privacy settings")
public record GamificationSettingsDto(
        @Schema(description = "Show progress to other household members") boolean showProgressToOthers,
        @Schema(description = "Enable gamification (points, badges, streaks)") boolean gamificationEnabled) {

    public static GamificationSettingsDto from(GamificationSettings settings) {
        return new GamificationSettingsDto(
                settings.isShowProgressToOthers(),
                settings.isGamificationEnabled());
    }

    public static GamificationSettingsDto defaults() {
        return new GamificationSettingsDto(true, true);
    }
}
```

---

## Task 4: Backend — Service

### File: `services/backend/src/main/java/com/hometusk/gamification/service/GamificationSettingsService.java`
```java
package com.hometusk.gamification.service;

import com.hometusk.gamification.domain.GamificationSettings;
import com.hometusk.gamification.dto.GamificationSettingsDto;
import com.hometusk.gamification.repository.GamificationSettingsRepository;
import com.hometusk.households.domain.Household;
import com.hometusk.users.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GamificationSettingsService {

    private static final Logger log = LoggerFactory.getLogger(GamificationSettingsService.class);

    private final GamificationSettingsRepository repository;

    public GamificationSettingsService(GamificationSettingsRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public GamificationSettings getOrCreate(User user, Household household) {
        return repository.findByUser_IdAndHousehold_Id(user.getId(), household.getId())
                .orElseGet(() -> {
                    try {
                        GamificationSettings settings = new GamificationSettings(user, household);
                        GamificationSettings saved = repository.save(settings);
                        log.info("Created gamification settings for user {} in household {}",
                                user.getId(), household.getId());
                        return saved;
                    } catch (DataIntegrityViolationException e) {
                        // Race condition: another request created it, re-fetch
                        log.debug("Settings already created by concurrent request, re-fetching");
                        return repository.findByUser_IdAndHousehold_Id(user.getId(), household.getId())
                                .orElseThrow(() -> new IllegalStateException("Settings should exist"));
                    }
                });
    }

    @Transactional
    public GamificationSettings update(User user, Household household, GamificationSettingsDto request) {
        GamificationSettings settings = getOrCreate(user, household);
        settings.setShowProgressToOthers(request.showProgressToOthers());
        settings.setGamificationEnabled(request.gamificationEnabled());
        return repository.save(settings);
    }

    public boolean isGamificationEnabled(User user, Household household) {
        return repository.findByUser_IdAndHousehold_Id(user.getId(), household.getId())
                .map(GamificationSettings::isGamificationEnabled)
                .orElse(true); // Default enabled
    }
}
```

---

## Task 5: Backend — Migration

### File: `services/backend/src/main/resources/db/migration/V019__add_gamification_settings.sql`
```sql
CREATE TABLE gamification_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    household_id UUID NOT NULL REFERENCES households(id),
    show_progress_to_others BOOLEAN NOT NULL DEFAULT TRUE,
    gamification_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, household_id)
);

CREATE INDEX idx_gamification_settings_user_household
    ON gamification_settings(user_id, household_id);
```

---

## Task 6: Backend — Controller Endpoints

### Modify: `GamificationController.java`

Add imports:
```java
import com.hometusk.gamification.dto.GamificationSettingsDto;
import com.hometusk.gamification.service.GamificationSettingsService;
import com.hometusk.households.domain.Household;
import com.hometusk.households.repository.HouseholdRepository;
import com.hometusk.users.domain.User;
import com.hometusk.users.repository.UserRepository;
```

Add to constructor:
```java
private final GamificationSettingsService settingsService;
private final UserRepository userRepository;
private final HouseholdRepository householdRepository;
```

Add endpoints:
```java
@GetMapping("/settings")
@Operation(summary = "Get user's gamification settings")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Settings"),
    @ApiResponse(responseCode = "403", description = "Not a member")
})
public ResponseEntity<GamificationSettingsDto> getSettings(@PathVariable UUID householdId) {
    CurrentUser currentUser = userResolver.resolveCurrentUser();
    membershipService.requireMembership(currentUser.id(), householdId);

    User user = userRepository.findById(currentUser.id())
            .orElseThrow(() -> new IllegalStateException("User not found"));
    Household household = householdRepository.findById(householdId)
            .orElseThrow(() -> new IllegalStateException("Household not found"));

    GamificationSettings settings = settingsService.getOrCreate(user, household);
    return ResponseEntity.ok(GamificationSettingsDto.from(settings));
}

@PutMapping("/settings")
@Operation(summary = "Update user's gamification settings")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Settings updated"),
    @ApiResponse(responseCode = "403", description = "Not a member")
})
public ResponseEntity<GamificationSettingsDto> updateSettings(
        @PathVariable UUID householdId,
        @RequestBody GamificationSettingsDto request) {
    CurrentUser currentUser = userResolver.resolveCurrentUser();
    membershipService.requireMembership(currentUser.id(), householdId);

    User user = userRepository.findById(currentUser.id())
            .orElseThrow(() -> new IllegalStateException("User not found"));
    Household household = householdRepository.findById(householdId)
            .orElseThrow(() -> new IllegalStateException("Household not found"));

    GamificationSettings settings = settingsService.update(user, household, request);
    return ResponseEntity.ok(GamificationSettingsDto.from(settings));
}
```

---

## Task 7: Backend — Service Enforcement

### Modify: `PointsService.java`

Add import and inject service in constructor:
```java
import com.hometusk.gamification.service.GamificationSettingsService;

// Add to constructor
private final GamificationSettingsService settingsService;
```

Add check at start of `awardForTaskCompleted()` method:
```java
@Transactional
public List<PointsLedger> awardForTaskCompleted(Task task, User actor) {
    // Check if gamification enabled for assignee
    if (!settingsService.isGamificationEnabled(task.getAssignee(), task.getHousehold())) {
        log.debug("Gamification disabled for user {}, skipping points award",
                task.getAssignee().getId());
        return List.of();
    }

    // ... existing logic (awardPointsIdempotent calls etc.)
}
```

**IMPORTANT:** Do NOT add check to `reverseForTaskUncompleted()` — it should always run to clean up points.

### Modify: `BadgeService.java`

Add import and inject service in constructor:
```java
import com.hometusk.gamification.service.GamificationSettingsService;

// Add to constructor
private final GamificationSettingsService settingsService;
```

Add check at start of `checkAndAwardBadges()` method:
```java
@Transactional
public void checkAndAwardBadges(User user, Household household) {
    if (!settingsService.isGamificationEnabled(user, household)) {
        log.debug("Gamification disabled for user {}, skipping badge check", user.getId());
        return;
    }

    // ... existing logic (loadEarnedBadges, checkAndAward calls etc.)
}
```

---

## Task 8: Backend — Integration Tests

### File: `services/backend/src/test/java/com/hometusk/integration/GamificationSettingsIntegrationTest.java`
```java
package com.hometusk.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

@DisplayName("Gamification Settings Integration Tests - ST-906")
class GamificationSettingsIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("GET /settings returns defaults for new user")
    void getSettings_newUser_returnsDefaults() throws Exception {
        mockMvc.perform(get("/api/v1/households/{id}/gamification/settings", testHousehold.getId())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.showProgressToOthers").value(true))
                .andExpect(jsonPath("$.gamificationEnabled").value(true));
    }

    @Test
    @DisplayName("PUT /settings updates and persists")
    void putSettings_updatesAndPersists() throws Exception {
        Map<String, Object> request = Map.of(
                "showProgressToOthers", false,
                "gamificationEnabled", false);

        mockMvc.perform(put("/api/v1/households/{id}/gamification/settings", testHousehold.getId())
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.showProgressToOthers").value(false))
                .andExpect(jsonPath("$.gamificationEnabled").value(false));

        // Verify persistence
        mockMvc.perform(get("/api/v1/households/{id}/gamification/settings", testHousehold.getId())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.showProgressToOthers").value(false))
                .andExpect(jsonPath("$.gamificationEnabled").value(false));
    }

    @Test
    @DisplayName("GET /settings returns 403 for non-member")
    void getSettings_notMember_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/households/{id}/gamification/settings", testHousehold.getId())
                        .with(jwtForUser(testUser2)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /settings returns 403 for non-member")
    void putSettings_notMember_returns403() throws Exception {
        Map<String, Object> request = Map.of(
                "showProgressToOthers", false,
                "gamificationEnabled", false);

        mockMvc.perform(put("/api/v1/households/{id}/gamification/settings", testHousehold.getId())
                        .with(jwtForUser(testUser2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
```

---

## Task 9: Web — Types

### Modify: `clients/web/src/types/api.ts`

Add GamificationSettings interface:
```typescript
export interface GamificationSettings {
  showProgressToOthers: boolean;
  gamificationEnabled: boolean;
}
```

---

## Task 10: Web — API Functions

### Modify: `clients/web/src/lib/api.ts`

Add imports and functions:
```typescript
import type { GamificationSettings } from '../types/api';

export async function getGamificationSettings(householdId: string): Promise<GamificationSettings> {
  return apiFetch<GamificationSettings>(`/households/${householdId}/gamification/settings`);
}

export async function updateGamificationSettings(
  householdId: string,
  settings: GamificationSettings
): Promise<GamificationSettings> {
  return apiFetch<GamificationSettings>(`/households/${householdId}/gamification/settings`, {
    method: 'PUT',
    body: settings,
  });
}
```

---

## Task 11: Web — Hook Update

### Modify: `clients/web/src/hooks/useGamification.ts`

Add settings fetch and update:
```typescript
import { getBadgeCatalog, getGamificationProgress, getGamificationSettings, updateGamificationSettings } from '../lib/api';
import type { BadgeCatalogResponse, GamificationProgress, GamificationSettings } from '../types/api';

export function useGamification(householdId: string | undefined) {
  const [progress, setProgress] = useState<GamificationProgress | null>(null);
  const [badges, setBadges] = useState<BadgeCatalogResponse | null>(null);
  const [settings, setSettings] = useState<GamificationSettings | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isUpdating, setIsUpdating] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const fetch = useCallback(async () => {
    if (!householdId) {
      setProgress(null);
      setBadges(null);
      setSettings(null);
      setIsLoading(false);
      setError(null);
      return;
    }

    setIsLoading(true);
    setError(null);
    try {
      const [progressData, badgesData, settingsData] = await Promise.all([
        getGamificationProgress(householdId),
        getBadgeCatalog(householdId),
        getGamificationSettings(householdId),
      ]);
      setProgress(progressData);
      setBadges(badgesData);
      setSettings(settingsData);
    } catch (err) {
      setError(err instanceof Error ? err : new Error('Failed to load gamification data'));
    } finally {
      setIsLoading(false);
    }
  }, [householdId]);

  const updateSettings = useCallback(async (newSettings: Partial<GamificationSettings>) => {
    if (!householdId || !settings) return;

    setIsUpdating(true);
    try {
      const updated = await updateGamificationSettings(householdId, {
        ...settings,
        ...newSettings,
      });
      setSettings(updated);
    } catch (err) {
      setError(err instanceof Error ? err : new Error('Failed to update settings'));
    } finally {
      setIsUpdating(false);
    }
  }, [householdId, settings]);

  useEffect(() => {
    fetch();
  }, [fetch]);

  return { progress, badges, settings, isLoading, isUpdating, error, refetch: fetch, updateSettings };
}
```

---

## Task 12: Web — Settings Component

### Create: `clients/web/src/components/gamification/PrivacySettingsCard.tsx`
```typescript
import type { GamificationSettings } from '../../types/api';

interface PrivacySettingsCardProps {
  settings: GamificationSettings;
  onUpdate: (settings: Partial<GamificationSettings>) => void;
  isUpdating: boolean;
}

export function PrivacySettingsCard({ settings, onUpdate, isUpdating }: PrivacySettingsCardProps) {
  return (
    <div className="progress__card privacy-settings">
      <h2>Privacy Settings</h2>

      <div className="privacy-settings__option">
        <label className="privacy-settings__label">
          <input
            type="checkbox"
            checked={settings.showProgressToOthers}
            onChange={(e) => onUpdate({ showProgressToOthers: e.target.checked })}
            disabled={isUpdating}
          />
          <span>Show my progress to household members</span>
        </label>
      </div>

      <div className="privacy-settings__option">
        <label className="privacy-settings__label">
          <input
            type="checkbox"
            checked={settings.gamificationEnabled}
            onChange={(e) => onUpdate({ gamificationEnabled: e.target.checked })}
            disabled={isUpdating}
          />
          <span>Enable gamification</span>
        </label>
        {!settings.gamificationEnabled && (
          <p className="privacy-settings__warning">
            You won't earn points or badges while gamification is disabled.
          </p>
        )}
      </div>

      {isUpdating && <p className="privacy-settings__saving">Saving...</p>}
    </div>
  );
}
```

### Update: `clients/web/src/components/gamification/index.ts`
```typescript
export { BadgeGrid } from './BadgeGrid';
export { PersonalProgressCard } from './PersonalProgressCard';
export { HouseholdAggregateCard } from './HouseholdAggregateCard';
export { PrivacySettingsCard } from './PrivacySettingsCard';
```

---

## Task 13: Web — Progress Page Update

### Modify: `clients/web/src/routes/Progress.tsx`

Update imports:
```typescript
import {
  HouseholdAggregateCard,
  PersonalProgressCard,
  BadgeGrid,
  PrivacySettingsCard,
} from '../components/gamification';
```

Update hook usage:
```typescript
const { progress, badges, settings, isLoading, isUpdating, error, refetch, updateSettings } = useGamification(activeId);
```

Add PrivacySettingsCard after badge catalog:
```tsx
{settings && (
  <PrivacySettingsCard
    settings={settings}
    onUpdate={updateSettings}
    isUpdating={isUpdating}
  />
)}
```

---

## Task 14: Web — CSS Addition

### Add to: `clients/web/src/routes/Progress.css`
```css
/* Privacy settings */
.privacy-settings__option {
  margin-bottom: 16px;
}

.privacy-settings__label {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.privacy-settings__label input[type="checkbox"] {
  width: 18px;
  height: 18px;
  cursor: pointer;
}

.privacy-settings__warning {
  margin: 8px 0 0 30px;
  font-size: var(--font-size-sm);
  color: var(--color-warning);
}

.privacy-settings__saving {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  font-style: italic;
}
```

---

## Verification Commands

```bash
# Backend
cd services/backend
./gradlew test --tests "*GamificationSettings*"
./gradlew test --tests "*Gamification*"
./gradlew spotlessApply

# Web
cd clients/web
npm run build
npm run lint
```

---

## DoD Checklist

- [ ] GamificationSettings entity created (with @ManyToOne)
- [ ] GamificationSettingsRepository created
- [ ] GamificationSettingsService created
- [ ] Migration V019 applied
- [ ] GET /settings endpoint works
- [ ] PUT /settings endpoint works
- [ ] Lazy-create on GET /settings works
- [ ] PointsService.awardForTaskCompleted checks gamificationEnabled
- [ ] PointsService.reverseForTaskUncompleted does NOT check (always runs)
- [ ] BadgeService.checkAndAwardBadges checks gamificationEnabled
- [ ] Integration tests pass (settings + security)
- [ ] GamificationSettings type added to web
- [ ] getGamificationSettings API function added
- [ ] updateGamificationSettings API function added
- [ ] useGamification includes settings + updateSettings + isUpdating
- [ ] PrivacySettingsCard renders
- [ ] Toggles update and persist
- [ ] Security: 403 for non-members on GET and PUT
- [ ] Spotless applied
- [ ] Web builds

---

## STOP-THE-LINE
If any of the following, STOP and report:
- Migration version conflict
- PointsService/BadgeService structure differs from expected
- Required dependencies missing
- Tests fail
