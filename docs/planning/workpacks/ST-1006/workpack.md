# Workpack: ST-1006 — Pause/Resume + Upcoming Instances View

## Sources of Truth
- Epic: `docs/planning/epics/EP-010/epic.md`
- Story: `docs/planning/epics/EP-010/stories/ST-1006-pause-resume-upcoming.md`
- Sprint: `docs/planning/pi/2026Q1-PI01/sprints/S12/sprint.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`
- OpenAPI: `docs/contracts/http/routines.openapi.yaml`
- Scheduler Service: `services/backend/src/main/java/com/hometusk/routines/service/RoutineSchedulerService.java`
- RecurrenceRuleParser: `services/backend/src/main/java/com/hometusk/routines/service/RecurrenceRuleParser.java`

---

## Status
**Ready** — DoR pass, ST-1005 is prerequisite

---

## Outcome

Users can pause/resume routines via UI buttons and see upcoming scheduled task instances for each routine.

---

## Acceptance Criteria Summary

1. **AC-1:** Pause ACTIVE routine -> status=PAUSED, pausedAt=now
2. **AC-2:** Pause idempotent (already PAUSED -> 200)
3. **AC-3:** Resume PAUSED routine -> status=ACTIVE, pausedAt=null
4. **AC-4:** Resume idempotent (already ACTIVE -> 200)
5. **AC-5:** Cannot pause/resume DELETED routine -> 400
6. **AC-6:** Upcoming instances endpoint returns 7 days
7. **AC-7:** Upcoming marks existing tasks
8. **AC-8:** Upcoming for PAUSED routine -> empty array
9. **AC-9:** UI pause button with confirmation
10. **AC-10:** UI resume button (immediate)
11. **AC-11:** Upcoming instances view in detail

---

## Files to Create

### Backend
| File | Description |
|------|-------------|
| `services/backend/src/main/java/com/hometusk/routines/dto/UpcomingInstanceDto.java` | DTO for upcoming instance |
| `services/backend/src/test/java/com/hometusk/routines/controller/RoutineLifecycleControllerTest.java` | Integration tests |

### Frontend
| File | Description |
|------|-------------|
| `clients/web/src/components/routines/UpcomingInstances.tsx` | Upcoming instances view |
| `clients/web/src/components/routines/PauseResumeButton.tsx` | Pause/Resume button |

---

## Files to Modify

### Backend
| File | Changes |
|------|---------|
| `services/backend/src/main/java/com/hometusk/routines/controller/RoutineController.java` | Add pause/resume/upcoming endpoints |
| `services/backend/src/main/java/com/hometusk/routines/service/RoutineService.java` | Add pause/resume/getUpcoming methods |
| `docs/contracts/http/routines.openapi.yaml` | Add new endpoints and schemas |

### Frontend
| File | Changes |
|------|---------|
| `clients/web/src/lib/api.ts` | Add pauseRoutine, resumeRoutine, getUpcomingInstances |
| `clients/web/src/types/api.ts` | Add UpcomingInstance type |
| `clients/web/src/components/routines/RoutineRow.tsx` | Add pause/resume buttons |
| `clients/web/src/routes/Routines.tsx` | Add upcoming instances view (detail/expandable) |

---

## Implementation Plan

### Step 1: Create UpcomingInstanceDto
**File:** `services/backend/src/main/java/com/hometusk/routines/dto/UpcomingInstanceDto.java`

```java
package com.hometusk.routines.dto;

import com.hometusk.users.dto.UserSummaryDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Upcoming task instance for a routine")
public record UpcomingInstanceDto(
        @Schema(description = "Scheduled date") LocalDate scheduledDate,
        @Schema(description = "Whether task already exists") boolean exists,
        @Schema(description = "Task ID if exists") UUID taskId,
        @Schema(description = "Projected assignee if not exists") UserSummaryDto projectedAssignee) {}
```

### Step 2: Add Service Methods
**File:** `services/backend/src/main/java/com/hometusk/routines/service/RoutineService.java`

Add methods:
```java
@Transactional
public Routine pause(UUID routineId, UUID householdId) {
    Routine routine = findByIdAndHousehold(routineId, householdId);
    if (routine.getStatus() == RoutineStatus.DELETED) {
        throw new IllegalStateException("Cannot change status of deleted routine");
    }
    if (routine.getStatus() != RoutineStatus.PAUSED) {
        routine.setStatus(RoutineStatus.PAUSED);
        routine.setPausedAt(Instant.now());
    }
    return routine;
}

@Transactional
public Routine resume(UUID routineId, UUID householdId) {
    Routine routine = findByIdAndHousehold(routineId, householdId);
    if (routine.getStatus() == RoutineStatus.DELETED) {
        throw new IllegalStateException("Cannot change status of deleted routine");
    }
    if (routine.getStatus() != RoutineStatus.ACTIVE) {
        routine.setStatus(RoutineStatus.ACTIVE);
        routine.setPausedAt(null);
    }
    return routine;
}

public List<UpcomingInstanceDto> getUpcomingInstances(UUID routineId, UUID householdId, int days) {
    Routine routine = findByIdAndHousehold(routineId, householdId);

    if (routine.getStatus() == RoutineStatus.PAUSED) {
        return List.of();
    }

    LocalDate today = LocalDate.now();
    List<LocalDate> dates = parseRecurrenceRule(routine)
        .stream()
        .filter(d -> !d.isBefore(today))
        .limit(days)
        .toList();

    // Check existing tasks for each date
    // Project assignee for future dates using AssignmentPolicyService
    // Return UpcomingInstanceDto list
}
```

### Step 3: Add Controller Endpoints
**File:** `services/backend/src/main/java/com/hometusk/routines/controller/RoutineController.java`

```java
@PostMapping("/{routineId}/pause")
public ResponseEntity<RoutineDto> pause(
        @PathVariable UUID householdId,
        @PathVariable UUID routineId,
        @AuthenticationPrincipal Jwt jwt) {
    membershipService.requireMembership(householdId, jwt);
    try {
        Routine routine = routineService.pause(routineId, householdId);
        return ResponseEntity.ok(RoutineDto.from(routine));
    } catch (IllegalStateException e) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}

@PostMapping("/{routineId}/resume")
public ResponseEntity<RoutineDto> resume(
        @PathVariable UUID householdId,
        @PathVariable UUID routineId,
        @AuthenticationPrincipal Jwt jwt) {
    membershipService.requireMembership(householdId, jwt);
    try {
        Routine routine = routineService.resume(routineId, householdId);
        return ResponseEntity.ok(RoutineDto.from(routine));
    } catch (IllegalStateException e) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}

@GetMapping("/{routineId}/upcoming")
public ResponseEntity<List<UpcomingInstanceDto>> getUpcoming(
        @PathVariable UUID householdId,
        @PathVariable UUID routineId,
        @RequestParam(defaultValue = "7") @Max(30) int days,
        @AuthenticationPrincipal Jwt jwt) {
    membershipService.requireMembership(householdId, jwt);
    List<UpcomingInstanceDto> instances = routineService.getUpcomingInstances(routineId, householdId, days);
    return ResponseEntity.ok(instances);
}
```

### Step 4: Update OpenAPI Contract
**File:** `docs/contracts/http/routines.openapi.yaml`

Add endpoints:
```yaml
/households/{householdId}/routines/{routineId}/pause:
  post:
    operationId: pauseRoutine
    summary: Pause a routine
    tags: [Routines]
    parameters:
      - $ref: '#/components/parameters/householdId'
      - $ref: '#/components/parameters/routineId'
    responses:
      '200':
        description: Routine paused
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/Routine'
      '400':
        description: Cannot pause (deleted routine)
      '403':
        description: Not a member
      '404':
        description: Routine not found

/households/{householdId}/routines/{routineId}/resume:
  post:
    operationId: resumeRoutine
    summary: Resume a paused routine
    tags: [Routines]
    # similar structure

/households/{householdId}/routines/{routineId}/upcoming:
  get:
    operationId: getUpcomingInstances
    summary: Get upcoming scheduled task instances
    tags: [Routines]
    parameters:
      - $ref: '#/components/parameters/householdId'
      - $ref: '#/components/parameters/routineId'
      - name: days
        in: query
        schema:
          type: integer
          default: 7
          maximum: 30
    responses:
      '200':
        description: List of upcoming instances
        content:
          application/json:
            schema:
              type: array
              items:
                $ref: '#/components/schemas/UpcomingInstance'

components:
  schemas:
    UpcomingInstance:
      type: object
      properties:
        scheduledDate:
          type: string
          format: date
        exists:
          type: boolean
        taskId:
          type: string
          format: uuid
          nullable: true
        projectedAssignee:
          $ref: '#/components/schemas/UserSummary'
          nullable: true
```

### Step 5: Add Frontend Types
**File:** `clients/web/src/types/api.ts`

```typescript
export interface UpcomingInstance {
  scheduledDate: string;
  exists: boolean;
  taskId?: string;
  projectedAssignee?: UserSummary;
}
```

### Step 6: Add Frontend API Functions
**File:** `clients/web/src/lib/api.ts`

```typescript
export async function pauseRoutine(householdId: string, routineId: string): Promise<Routine> {
  return apiFetch<Routine>(`/households/${householdId}/routines/${routineId}/pause`, {
    method: 'POST',
  });
}

export async function resumeRoutine(householdId: string, routineId: string): Promise<Routine> {
  return apiFetch<Routine>(`/households/${householdId}/routines/${routineId}/resume`, {
    method: 'POST',
  });
}

export async function getUpcomingInstances(
  householdId: string,
  routineId: string,
  days: number = 7
): Promise<UpcomingInstance[]> {
  return apiFetch<UpcomingInstance[]>(
    `/households/${householdId}/routines/${routineId}/upcoming?days=${days}`
  );
}
```

### Step 7: Create PauseResumeButton Component
**File:** `clients/web/src/components/routines/PauseResumeButton.tsx`

```tsx
interface Props {
  routine: Routine;
  onPause: () => void;
  onResume: () => void;
}

export default function PauseResumeButton({ routine, onPause, onResume }: Props) {
  const [showConfirm, setShowConfirm] = useState(false);

  if (routine.status === 'DELETED') return null;

  if (routine.status === 'PAUSED') {
    return (
      <Button variant="secondary" size="sm" onClick={onResume}>
        Resume
      </Button>
    );
  }

  return (
    <>
      <Button variant="secondary" size="sm" onClick={() => setShowConfirm(true)}>
        Pause
      </Button>
      {showConfirm && (
        <Modal onClose={() => setShowConfirm(false)}>
          <p>Pause routine? No new tasks will be generated.</p>
          <Button onClick={onPause}>Confirm Pause</Button>
        </Modal>
      )}
    </>
  );
}
```

### Step 8: Create UpcomingInstances Component
**File:** `clients/web/src/components/routines/UpcomingInstances.tsx`

```tsx
interface Props {
  householdId: string;
  routineId: string;
}

export default function UpcomingInstances({ householdId, routineId }: Props) {
  const [instances, setInstances] = useState<UpcomingInstance[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    getUpcomingInstances(householdId, routineId)
      .then(setInstances)
      .finally(() => setIsLoading(false));
  }, [householdId, routineId]);

  if (isLoading) return <Skeleton />;
  if (instances.length === 0) return <p>No upcoming tasks (routine paused)</p>;

  return (
    <div className="upcoming-instances">
      <h4>Upcoming Tasks</h4>
      <ul>
        {instances.map(inst => (
          <li key={inst.scheduledDate}>
            <span>{inst.scheduledDate}</span>
            {inst.exists ? (
              <Link to={`tasks/${inst.taskId}`}>
                <Badge variant="success">Created</Badge>
              </Link>
            ) : (
              <span>Assigned to: {inst.projectedAssignee?.displayName || 'TBD'}</span>
            )}
          </li>
        ))}
      </ul>
    </div>
  );
}
```

### Step 9: Update RoutineRow with Pause/Resume
**File:** `clients/web/src/components/routines/RoutineRow.tsx`

Add PauseResumeButton and handle callbacks to refetch list.

### Step 10: Add Upcoming View to Routines Page
**File:** `clients/web/src/routes/Routines.tsx`

Add expandable row or detail view showing UpcomingInstances component.

### Step 11: Write Integration Tests
**File:** `services/backend/src/test/java/com/hometusk/routines/controller/RoutineLifecycleControllerTest.java`

Test cases:
- `pause_activeRoutine_changesStatus`
- `pause_alreadyPaused_returns200`
- `pause_deletedRoutine_returns400`
- `resume_pausedRoutine_changesStatus`
- `resume_alreadyActive_returns200`
- `resume_deletedRoutine_returns400`
- `upcoming_activeRoutine_returnsInstances`
- `upcoming_pausedRoutine_returnsEmpty`
- `upcoming_marksExistingTasks`

---

## Verification Commands

```bash
# Backend build
cd services/backend && ./gradlew build

# Backend tests
cd services/backend && ./gradlew test --tests "*RoutineLifecycleControllerTest"

# Frontend build
cd clients/web && npm run build

# Frontend type check
cd clients/web && npm run typecheck

# Spotless
cd services/backend && ./gradlew spotlessApply
```

---

## Tests

### Unit Tests (Backend)
- `RoutineService.pause()` state transitions
- `RoutineService.resume()` state transitions
- `RoutineService.getUpcomingInstances()` date calculation

### Integration Tests (Backend)
- `pause_activeRoutine_stopsGeneration`
- `resume_pausedRoutine_resumesGeneration`
- `upcoming_showsProjectedAssignments`
- `upcoming_marksExistingTasks`
- `upcoming_respectsHouseholdBoundary`

### Manual Tests (Frontend)
- Click pause on ACTIVE routine
- Verify confirmation dialog
- Verify status changes to PAUSED
- Click resume on PAUSED routine
- Verify status changes to ACTIVE
- Expand routine to see upcoming instances
- Verify created tasks show link

---

## DoD Checklist

### Backend
- [ ] `UpcomingInstanceDto` created
- [ ] `pause()` method in RoutineService
- [ ] `resume()` method in RoutineService
- [ ] `getUpcomingInstances()` method in RoutineService
- [ ] Controller endpoints added
- [ ] OpenAPI contract updated
- [ ] Integration tests passing
- [ ] Spotless applied
- [ ] `./gradlew build` passes

### Frontend
- [ ] `UpcomingInstance` type added
- [ ] API functions added
- [ ] `PauseResumeButton` component
- [ ] `UpcomingInstances` component
- [ ] RoutineRow updated with buttons
- [ ] Upcoming view in Routines page
- [ ] `npm run build` passes
- [ ] `npm run typecheck` passes

---

## Risks

| Risk | Mitigation |
|------|------------|
| Round-robin projection complexity | Simulate without mutating state |
| Existing tasks lookup performance | Query with IN clause, limit to window |
| UI state consistency after pause/resume | Refetch list after action |

---

## Rollback

If issues discovered:
1. Revert controller endpoints
2. Revert service methods
3. Revert OpenAPI changes
4. Frontend: remove new components, keep types/API functions
