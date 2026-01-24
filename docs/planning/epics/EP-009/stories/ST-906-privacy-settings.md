# Story: ST-906 — Privacy Settings + Opt-out

## Sources of Truth
- Epic: `docs/planning/epics/EP-009/epic.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Deferred** — OUT OF SCOPE for S08 (planned for S09)

**Rationale:** S08 UI shows only aggregate household data, so privacy toggle not needed yet. Individual breakdown requires privacy controls to be implemented first.

## User Value
> "Не хочу чтобы другие видели мой прогресс — включаю 'скрыть' и спокоен."

---

## Description
Implement privacy controls for gamification:
- Toggle: "Show my progress to others"
- Toggle: "Enable gamification" (full opt-out)
- Settings page or modal
- Respects toggles in all views

---

## Acceptance Criteria

### AC-1: Privacy toggle exists
```
Given user navigates to settings (or progress page)
When user sees gamification settings
Then toggle "Show my progress to others" is visible
And default = ON (visible)
```

### AC-2: Toggle hides from household view
```
Given user sets showProgressToOthers = false
When other member views household progress
Then user not visible in member breakdown
But user's points still count in aggregate
```

### AC-3: Full opt-out toggle
```
Given user sets gamificationEnabled = false
When user completes task
Then no points awarded
And no streak updated
And no badges awarded
```

### AC-4: Opt-out reversible
```
Given user re-enables gamification
When user completes task
Then points awarded normally
And streak starts fresh
```

### AC-5: Settings persisted
```
When user changes settings
Then PUT /households/{id}/gamification/settings
And settings persisted to DB
And reflected on next load
```

### AC-6: Settings API
```
PUT /api/v1/households/{householdId}/gamification/settings
Body:
{
  "showProgressToOthers": false,
  "gamificationEnabled": true
}
Response: 200 OK
```

---

## Domain Impact

### New Entity
```java
@Entity
public class GamificationSettings {
    @Id
    private UUID id;
    private UUID userId;
    private UUID householdId;
    private boolean showProgressToOthers = true;
    private boolean gamificationEnabled = true;
    private Instant updatedAt;
}
```

### New Endpoint
- `PUT /api/v1/households/{id}/gamification/settings`

---

## Contract Impact
**Yes** — New settings endpoint

---

## DB Migration
```sql
CREATE TABLE gamification_settings (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    household_id UUID NOT NULL REFERENCES households(id),
    show_progress_to_others BOOLEAN NOT NULL DEFAULT TRUE,
    gamification_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, household_id)
);
```

---

## UI Components
- Settings section on Progress page (or separate settings modal)
- Toggle switches with clear labels

---

## Points
**3 points**

---

## Flags
- contract_impact: yes
