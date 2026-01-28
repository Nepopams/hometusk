# Workpack: ST-906 — Privacy Settings + Opt-out

## Sources of Truth
- Epic: `docs/planning/epics/EP-009/epic.md`
- Story: `docs/planning/epics/EP-009/stories/ST-906-privacy-settings.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Ready** — S09 Scope

---

## Outcome
Privacy controls for gamification:
1. Backend: GamificationSettings entity + PUT endpoint
2. Web: Settings toggle UI on Progress page
3. Enforcement: hidden users excluded from breakdown (future), points still count in aggregate

---

## Key Decisions
- Settings per-user per-household
- Default: `showProgressToOthers = true`, `gamificationEnabled = true`
- Full opt-out stops points/badges/streak accrual
- Settings reversible
- Aggregate always includes all (even hidden users)

---

## Scope

### In Scope
- GamificationSettings entity + repository
- `PUT /api/v1/households/{id}/gamification/settings` endpoint
- Settings UI toggle on Progress page
- gamificationEnabled flag enforcement in PointsService
- Security tests (403 for non-members, no cross-household)

### Out of Scope
- Individual member breakdown in UI (requires settings first, but separate story)
- Admin override of settings
- Push notification preferences

---

## Files to Create

### Backend
| Path | Purpose |
|------|---------|
| `services/backend/src/main/java/com/hometusk/gamification/domain/GamificationSettings.java` | Entity |
| `services/backend/src/main/java/com/hometusk/gamification/repository/GamificationSettingsRepository.java` | Repository |
| `services/backend/src/main/java/com/hometusk/gamification/dto/GamificationSettingsDto.java` | Request/Response DTO |
| `services/backend/src/main/resources/db/migration/V019__add_gamification_settings.sql` | Migration |

### Web
| Path | Purpose |
|------|---------|
| `clients/web/src/components/gamification/PrivacySettingsCard.tsx` | Settings toggle UI |

### Files to Modify

#### Backend
| Path | Changes |
|------|---------|
| `GamificationController.java` | Add PUT /settings endpoint, GET settings |
| `PointsService.java` | Check gamificationEnabled before awarding |
| `BadgeService.java` | Check gamificationEnabled before awarding |

#### Web
| Path | Changes |
|------|---------|
| `clients/web/src/types/api.ts` | Add GamificationSettings type |
| `clients/web/src/lib/api.ts` | Add getSettings, updateSettings |
| `clients/web/src/hooks/useGamification.ts` | Include settings in fetch |
| `clients/web/src/routes/Progress.tsx` | Add PrivacySettingsCard |

---

## API Contract

### New Endpoint
```yaml
PUT /api/v1/households/{householdId}/gamification/settings
  requestBody:
    content:
      application/json:
        schema:
          type: object
          properties:
            showProgressToOthers:
              type: boolean
            gamificationEnabled:
              type: boolean
  responses:
    200:
      description: Settings updated
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/GamificationSettingsDto'
    403:
      description: Not a member
```

### GET (optional, can be part of progress response)
```yaml
GET /api/v1/households/{householdId}/gamification/settings
  responses:
    200:
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/GamificationSettingsDto'
```

---

## DB Migration

```sql
-- V019__add_gamification_settings.sql

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

## Implementation Notes

### Settings Enforcement
```java
// PointsService.awardPoints()
GamificationSettings settings = settingsRepository
    .findByUserIdAndHouseholdId(userId, householdId)
    .orElse(GamificationSettings.defaults(userId, householdId));

if (!settings.isGamificationEnabled()) {
    log.debug("Gamification disabled for user {}, skipping points", userId);
    return;
}
// ... proceed with points
```

### UI Toggle Component
```tsx
// PrivacySettingsCard.tsx
interface PrivacySettingsCardProps {
  settings: GamificationSettings;
  onUpdate: (settings: Partial<GamificationSettings>) => void;
  isUpdating: boolean;
}

// Toggles:
// - "Show my progress to household members" (showProgressToOthers)
// - "Enable gamification" (gamificationEnabled)
// Warning text when disabling: "You won't earn points or badges while disabled."
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

## Risks

| Risk | Mitigation |
|------|------------|
| Settings not created on first access | Lazy-create with defaults |
| Race condition on update | Optimistic locking / last-write-wins |
| Confusion about aggregate vs individual | Clear UI copy |

---

## DoD Checklist

- [ ] GamificationSettings entity created
- [ ] Migration applied
- [ ] PUT endpoint works
- [ ] GET endpoint works (or included in progress)
- [ ] PointsService respects gamificationEnabled
- [ ] BadgeService respects gamificationEnabled
- [ ] UI toggle renders
- [ ] Toggle updates persist
- [ ] Security tests pass (403 for non-members)
- [ ] Spotless applied
- [ ] Web builds
