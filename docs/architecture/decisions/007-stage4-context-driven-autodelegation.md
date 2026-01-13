# ADR-007: Stage 4 Context-driven Autodelegation

**Status:** Accepted
**Date:** 2026-01-13
**Deciders:** HomeTusk Development Team
**Related:** ADR-004 (AI Platform Integration), ADR-006 (Upstream Contract Alignment)

---

## Context and Problem Statement

Stage 3 established the guardrails pipeline foundation with two basic policies. Stage 4 extends this with **intelligent task autodelegation** based on:
- Member workload capacity
- Zone ownership preferences
- Deadline reasonableness
- Member availability (future)

**Key requirements:**
1. Calculate member workload scores (0.0-1.0) for AI Platform context
2. Add zone ownership tracking for preference-based assignment
3. Validate assignee membership, deadline sanity, and availability
4. Implement comprehensive observability for decision pipeline
5. Use 200 OK with status discriminator for clarification requests (not 422)

**Constraints:**
- Minimal zone owner implementation (schema only, no full API/UX)
- Availability checking behind feature flag (OFF by default, Stage 5)
- Backward compatibility with existing clients
- No upstream contract changes

---

## Decision Drivers

- **Business Value:** Natural language → assigned task with intelligent defaults
- **User Experience:** Reduce manual assignee selection, respect member capacity
- **Observability:** Full metrics for decision pipeline telemetry
- **Safety:** Fail-safe policies prevent overload, invalid assignments, unrealistic deadlines
- **Future-Proofing:** Feature flags enable Stage 5 enhancements without code changes

---

## Considered Options

### Option 1: Full Zone Owner Management (REJECTED)
**Description:** Implement complete zone ownership API/UX with CRUD operations, UI, and role-based access.

**Pros:**
- Complete feature in one stage
- Immediate user value

**Cons:**
- Scope creep beyond Stage 4 goals
- Requires frontend changes
- Delays core autodelegation features

**Decision:** REJECTED. Stage 4 adds schema only; owner management deferred to Stage 5.

---

### Option 2: Global Quiet Hours Configuration (REJECTED)
**Description:** Use application.yml for quiet hours (22:00-07:00) applied to all households.

**Pros:**
- Simple implementation
- No database changes needed

**Cons:**
- Ignores timezone differences across households
- No per-household customization
- Poor user experience for global settings

**Decision:** REJECTED. Use feature flag approach with household-level config in Stage 5.

---

### Option 3: NEEDS_INPUT as 422 Unprocessable Entity (REJECTED)
**Description:** Return HTTP 422 when guardrails request clarification.

**Pros:**
- Semantically distinct from success (200)
- Common pattern for validation errors

**Cons:**
- NEEDS_INPUT is not a validation error (user input was valid)
- Inconsistent with Command API pattern (200=success/clarify, 207=degraded, 400=error)
- Breaks existing client expectations (clients check `status` field, not HTTP code)

**Decision:** REJECTED. Use 200 OK with `status: "needs_input"` discriminator.

---

## Decision Outcome

**Chosen approach:** Incremental Stage 4 implementation with feature flags and status discriminators.

### Implementation Strategy

#### 1. Database Schema (ACCEPTED)

**Decision:** Add `zones.owner_id` column with NULL support.

```sql
ALTER TABLE zones ADD COLUMN owner_id UUID;
ALTER TABLE zones ADD CONSTRAINT fk_zones_owner
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL;
CREATE INDEX idx_zones_owner_id ON zones(owner_id) WHERE owner_id IS NOT NULL;
```

**Rationale:**
- Minimal schema addition enables intelligent defaults
- NULL support ensures backward compatibility
- ON DELETE SET NULL prevents cascading failures
- Index optimizes ZoneOwnerFirstPolicy queries

**Safe Degradation:**
- Existing zones continue with owner_id=NULL
- ZoneOwnerFirstPolicy skips assignment if no owner
- No API/UX exposure in Stage 4

---

#### 2. Workload Score Calculation (ACCEPTED)

**Decision:** Calculate `workload_score` in ContextBuilder as `openTasks / maxTasks`, clamped to [0.0, 1.0].

**Algorithm:**
```java
private double calculateWorkloadScore(int openTasks, int maxTasks) {
    if (maxTasks <= 0) maxTasks = 10; // Guard against invalid config
    double score = (double) openTasks / maxTasks;
    return Math.min(1.0, Math.max(0.0, score)); // Clamp to [0.0, 1.0]
}
```

**Rationale:**
- Simple, deterministic formula
- Linear scaling with clear semantics (0.0=no tasks, 1.0=at capacity)
- Guards against division by zero
- Clamping handles over-capacity gracefully

**Included in AI Platform Context:**
```json
{
  "members": [
    {
      "id": "uuid",
      "name": "Maria",
      "workload_score": 0.8
    }
  ]
}
```

**Alternative Considered:** Exponential scaling (e.g., sigmoid) for non-linear penalty.
**Rejected:** Adds complexity without clear user benefit. Linear is sufficient for MVP.

---

#### 3. New Guardrail Policies (ACCEPTED)

**Decision:** Add 3 new policies with specific execution order.

##### Policy Execution Order

| Order | Policy | Purpose | Outcome Types |
|-------|--------|---------|---------------|
| 50 | MembershipPolicy | Reject invalid assignees | ACCEPT, REJECT |
| 100 | ZoneOwnerFirstPolicy | Assign zone owner if no assignee | ACCEPT, MODIFY |
| 150 | DeadlineSanityPolicy | Validate deadline reasonableness | ACCEPT, CLARIFY |
| 200 | MaxOpenTasksPerAssigneePolicy | Check workload capacity | ACCEPT, CLARIFY |
| 250 | AvailabilityPolicy | Check quiet hours (feature flag) | ACCEPT, CLARIFY |

**Order Rationale:**
1. **MembershipPolicy first (50):** Reject early before processing invalid assignees
2. **ZoneOwnerFirstPolicy second (100):** Assign owner before workload checks
3. **DeadlineSanityPolicy third (150):** Validate deadline before capacity checks
4. **MaxOpenTasksPerAssigneePolicy fourth (200):** Check workload after assignee determined
5. **AvailabilityPolicy last (250):** Check availability after all other validations

##### MembershipPolicy (Order 50)

**Purpose:** Validate assignee is a household member.

**Behavior:**
- Extract assigneeId from create_task action
- Check if assigneeId exists in HouseholdSnapshot.members
- REJECT if not found: "Выбранный пользователь не является участником этого домохозяйства" (errorCode: ASSIGNEE_NOT_MEMBER)
- ACCEPT otherwise

**Rationale:**
- Prevents IDOR vulnerabilities (assigning to users outside household)
- Fail-fast approach (reject before expensive operations)
- Clear error message for debugging

##### DeadlineSanityPolicy (Order 150)

**Purpose:** Validate deadline is reasonable (not past, not > 365 days).

**Behavior:**
- Extract deadline from create_task action
- CLARIFY if deadline < now: "Указанный срок уже прошёл. Пожалуйста, выберите будущую дату."
- CLARIFY if deadline > now + maxDeadlineDays: "Указанный срок слишком далеко в будущем. Пожалуйста, выберите дату в пределах года."
- ACCEPT otherwise

**Configuration:**
```yaml
guardrails:
  max-deadline-days: ${GUARDRAILS_MAX_DEADLINE_DAYS:365}
```

**Rationale:**
- Catches AI misinterpretations ("завтра" parsed as yesterday)
- Prevents unrealistic deadlines (e.g., 2050-01-01)
- CLARIFY (not REJECT) allows user to confirm/adjust

##### AvailabilityPolicy (Order 250) - FEATURE FLAG

**Decision:** Implement with feature flag `guardrails.availability.enabled=false` (default OFF).

**Behavior (when enabled):**
- Parse quiet-hours-start and quiet-hours-end from config (LocalTime)
- Convert deadline to LocalTime (system timezone)
- Check if falls in quiet hours range (handle midnight wrap: 22:00-07:00)
- CLARIFY if in quiet hours: "Задача назначена на ночное время. Подтвердите или измените срок."
- Provide suggestions: `{"deadline": "<next morning 09:00>"}`

**Behavior (when disabled - default):**
```java
if (!config.isAvailabilityEnabled()) {
    log.debug("AvailabilityPolicy disabled (feature flag OFF), skipping check");
    return GuardrailOutcome.accept();
}
```

**Configuration:**
```yaml
guardrails:
  availability-enabled: ${GUARDRAILS_AVAILABILITY_ENABLED:false}  # OFF by default
  quiet-hours-start: ${GUARDRAILS_QUIET_HOURS_START:22:00}
  quiet-hours-end: ${GUARDRAILS_QUIET_HOURS_END:07:00}
```

**Rationale for Feature Flag:**
- Quiet hours should be **household-level**, not global
- Stage 4 lacks household_settings table for per-household config
- Global quiet hours ignore timezone differences across households
- Feature flag enables Stage 5 activation without code changes

**Stage 5 Migration Path:**
1. Add household_settings table with quiet_hours_start/end columns
2. Update GuardrailsConfig to read from household_settings (not application.yml)
3. Enable feature flag by default: `availability-enabled: true`
4. Document in ADR-008 (Stage 5)

**Alternative Considered:** Hardcode quiet hours in Stage 4, refactor in Stage 5.
**Rejected:** Feature flag approach is cleaner and avoids breaking changes.

---

#### 4. Observability Metrics (ACCEPTED)

**Decision:** Add DecisionMetrics component with 5 Micrometer metrics.

**Metrics:**

| Metric | Type | Tags | Purpose |
|--------|------|------|---------|
| decision.latency | Timer | source={aiplatform\|manual\|fallback} | Track decision pipeline performance |
| decision.outcome | Counter | outcome={applied\|clarify\|reject\|degraded} | Track final decision outcomes |
| decision.source | Counter | source={aiplatform\|manual\|fallback} | Track decision provider usage |
| guardrails.outcome | Counter | policy={name}, outcome={accept\|clarify\|reject\|modify} | Track per-policy outcomes |
| aiplatform.latency | Timer | endpoint={/decision\|/decide}, status={success\|error\|timeout} | Track AI Platform performance |

**Integration Points:**
- **DecisionProviderSelector:** Records decision.latency, decision.source
- **GuardrailsOrchestrator:** Records guardrails.outcome per policy
- **CommandService:** Records decision.outcome (final result)
- **AiPlatformAdapter:** Records aiplatform.latency (future)

**Rationale:**
- Enables SLO monitoring (p99 latency < 100ms)
- Identifies bottlenecks in pipeline (which policies are slow?)
- Tracks degraded mode frequency (fallback usage)
- Supports A/B testing (compare policy variants)

**Example Queries (Prometheus):**
```promql
# P99 decision latency by source
histogram_quantile(0.99, sum(rate(decision_latency_seconds_bucket[5m])) by (source, le))

# Clarification rate by policy
sum(rate(guardrails_outcome_total{outcome="clarify"}[5m])) by (policy)

# Degraded mode percentage
sum(rate(decision_outcome_total{outcome="degraded"}[5m]))
  / sum(rate(decision_outcome_total[5m]))
```

---

#### 5. NEEDS_INPUT Response Format (ACCEPTED)

**Decision:** Use 200 OK with `status: "needs_input"` discriminator (not 422).

**OpenAPI Schema:**
```yaml
CommandSuccessResponse:
  type: object
  required:
    - commandId
    - correlationId
    - status
    - executionMs
  properties:
    status:
      type: string
      enum:
        - executed
        - needs_input  # NEW for Stage 4
    question:
      type: string
      description: Present when status=needs_input
    requiredFields:
      type: array
      items: {type: string}
      description: Present when status=needs_input
    suggestions:
      type: object
      description: Present when status=needs_input
    policyName:
      type: string
      description: Present when status=needs_input (guardrail that requested clarification)
```

**Example Response:**
```json
{
  "commandId": "cmd-uuid",
  "correlationId": "corr-uuid",
  "status": "needs_input",
  "question": "У пользователя Maria уже 10 открытых задач. Назначить на другого участника?",
  "requiredFields": ["assigneeId"],
  "suggestions": {
    "assignees": [
      {"id": "user-2", "name": "Alex", "workload": 0.3}
    ]
  },
  "policyName": "MaxOpenTasksPerAssignee",
  "executionMs": 45
}
```

**Rationale:**
1. **Consistent with existing pattern:**
   - 200 OK = success or clarify (command processed successfully)
   - 207 Multi-Status = degraded (executed but with warnings)
   - 400 Bad Request = client error (invalid input)
   - 422 Unprocessable Entity = validation failure

2. **NEEDS_INPUT is not a validation error:**
   - User input was valid and passed schema + business validation
   - Guardrails requested clarification based on household state
   - This is a successful processing outcome, not an error

3. **Backward compatibility:**
   - Existing clients already check `status` field (not HTTP code)
   - Adding `needs_input` to enum does not break clients
   - Clients that don't handle needs_input will see error in status check

4. **Semantic correctness:**
   - 422 implies "I cannot process this" (permanent failure)
   - 200 with needs_input implies "I processed this and need more info" (temporary state)

**Alternative Considered:** 202 Accepted (async processing)
**Rejected:** 202 implies background processing, but clarification is immediate.

**policyName Field:**
**Decision:** Add `policyName` to CommandNeedsInputResponse.

**Rationale:**
- Enables debugging (which policy requested clarification?)
- Supports policy-specific UX (e.g., show workload chart for MaxOpenTasksPerAssigneePolicy)
- Facilitates metrics (track clarification rate per policy)

---

## Consequences

### Positive

1. **Intelligent Autodelegation:**
   - Tasks assigned to members with capacity
   - Zone owners preferred for their zones
   - Invalid deadlines caught with helpful suggestions

2. **Comprehensive Observability:**
   - Full metrics for decision pipeline telemetry
   - Enables SLO monitoring and bottleneck identification
   - Supports data-driven optimization

3. **Safe Degradation:**
   - Zones without owners continue working
   - AvailabilityPolicy disabled by default (no impact)
   - Feature flags enable Stage 5 enhancements

4. **Backward Compatibility:**
   - Existing zones continue with owner_id=NULL
   - 200 OK with status field consistent with existing pattern
   - No API breaking changes

5. **Clear Policy Boundaries:**
   - Each policy has single responsibility
   - Execution order documented and justified
   - Easy to test and maintain

### Negative

1. **Zone Owner Incomplete:**
   - No API/UX for setting zone owners
   - Requires database access or Stage 5 features
   - Partial implementation may confuse users

   **Mitigation:** Document as Stage 5 feature, owner_id is internal optimization.

2. **Quiet Hours Not Per-Household:**
   - Global quiet hours ignore timezones
   - Cannot customize per household
   - Feature flag OFF by default (no immediate value)

   **Mitigation:** Stage 5 adds household_settings table and per-household config.

3. **Workload Score Formula Simple:**
   - Linear scaling may not reflect true capacity
   - No consideration of task complexity or duration
   - May require tuning in production

   **Mitigation:** Monitor metrics, iterate formula in Stage 5 if needed.

4. **Policy Order Coupling:**
   - Policies must run in specific order
   - Changing order may break assumptions
   - Requires careful ADR review for future policies

   **Mitigation:** Document order rationale, enforce with unit tests.

### Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Zone owner migration breaks existing code | Medium | Use ON DELETE SET NULL, null checks in policies |
| Workload score divide-by-zero | Low | Guard: if (maxTasks <= 0) maxTasks = 10 |
| Quiet hours timezone issues | Medium | Use ZoneId.systemDefault() for Stage 4, defer household timezones to Stage 5 |
| New policies break existing tests | Medium | Run full test suite after each policy |
| Metrics overhead impacts performance | Low | Micrometer efficient registry, avoid high-cardinality tags |

---

## Validation

### Integration Tests (9 scenarios)

1. ✅ Decision with assignee + deadline → Task created
2. ✅ Assignee not member → REJECT (MembershipPolicy)
3. ✅ Deadline in past → CLARIFY (DeadlineSanityPolicy)
4. ✅ Deadline > 365 days → CLARIFY (DeadlineSanityPolicy)
5. ✅ Quiet hours + AvailabilityPolicy disabled → ACCEPT (feature flag OFF)
6. ✅ Max open tasks → CLARIFY (MaxOpenTasksPerAssigneePolicy)
7. ✅ Zone with owner, no assignee → Owner assigned (ZoneOwnerFirstPolicy)
8. ✅ Workload score in context → WireMock verification
9. ✅ Full chain test → Upstream → Guardrails → Action execution

### End-to-End Verification

**Scenario:** "Убрать кухню сегодня вечером"

**Expected Flow:**
1. AI Platform returns: zone=kitchen, no assignee, due_at=23:00
2. ZoneOwnerFirstPolicy assigns kitchen owner
3. AvailabilityPolicy skipped (feature flag OFF)
4. DeadlineSanityPolicy accepts (future date)
5. MaxOpenTasksPerAssigneePolicy checks workload
6. Task created with: assignee=kitchen owner, deadline=23:00
7. Metrics recorded: decision.latency, guardrails.outcome (3x), decision.outcome=applied

**Metrics Verification:**
```bash
curl http://localhost:8080/actuator/metrics/decision.latency
curl http://localhost:8080/actuator/metrics/guardrails.outcome
```

---

## Related Decisions

- **ADR-004:** AI Platform Integration (decision provider abstraction)
- **ADR-006:** Upstream Contract Alignment (field mapping, endpoint configuration)
- **ADR-008 (Future):** Stage 5 Household Settings (quiet hours, zone owner management)

---

## References

- Stage 4 Implementation Plan: `/root/.claude/plans/deep-chasing-fairy.md`
- Commands API Contract: `docs/contracts/http/commands.openapi.yaml`
- AI Platform Integration: `docs/integration/ai-platform/v1/`
- ContextBuilder: `services/backend/src/main/java/com/hometusk/commands/pipeline/ContextBuilder.java`
- GuardrailsOrchestrator: `services/backend/src/main/java/com/hometusk/commands/pipeline/guardrails/GuardrailsOrchestrator.java`
- Integration Tests: `services/backend/src/test/java/com/hometusk/integration/guardrails/Stage4GuardrailsIntegrationTest.java`

---

## Notes

**Feature Flag Strategy:**
- AvailabilityPolicy demonstrates pattern for Stage 5 features
- Feature flags enable incremental rollout without code changes
- Document disabled features in CLAUDE.md for future reference

**Policy Ordering Principle:**
- Fail-fast policies first (reject invalid input early)
- Modify policies before validation (assign assignee before checking workload)
- Expensive policies last (availability checks after all other validations)

**Status Discriminator Pattern:**
- Prefer status field discriminators over HTTP codes for domain states
- Reserve HTTP codes for transport-level concerns (success, client error, server error)
- Enables richer responses without breaking REST semantics
