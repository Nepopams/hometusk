# Sprint S15 — Voice Input Core Flow (Web)

## Sources of Truth
- Product Goal: `docs/planning/strategy/product-goal.md`
- Scope Anchor: `docs/planning/initiatives/INIT-2026Q2-voice-input-web.md`
- Epic: `docs/planning/epics/EP-012/epic.md`
- PI Charter: `docs/planning/pi/2026Q1-PI01/pi.md`
- Previous Sprint: `docs/planning/pi/2026Q1-PI01/sprints/S14/sprint.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`
- ASR Proxy Contract: `docs/contracts/http/asr-proxy.openapi.yaml`

---

## Goal

**Deliver core voice input flow: record -> upload -> transcribe -> edit -> submit.**

Enable web users to record voice via microphone button, send audio to ASR proxy, receive transcript in editable input field, and submit as a normal command. This is the essential E2E path for voice input without polish/hardening.

**Success Metric:**
- User can click mic, speak, see transcript, edit if needed, submit command
- Recording state visible with timer, cancel possible
- Transcript populates existing CommandInput field
- Works in Chrome (primary), Firefox, Safari as stretch

**Product Goal Alignment:** Fairness & Transparency (lower barrier to task creation via voice)

---

## Prioritization Rationale

This sprint delivers EP-012 core functionality (Milestone M1):

1. **ASR Foundation complete (EP-011):** Backend proxy ready, all guardrails in place
2. **S15 = Milestone M1:** "Core voice flow works" - record, upload, transcribe, edit, submit
3. **Stories sequenced:** ST-1201 -> ST-1202 -> ST-1203 -> ST-1204 -> ST-1205 (integration)

**Why S15 now:**
- ASR proxy production-ready (S14 complete)
- Voice input is NOW priority in roadmap
- Core flow must work before error handling / polish (S16)

---

## Scope

### Committed (DoR-ready)

| Story | Title | Points | Priority | Dependencies | Flags |
|-------|-------|--------|----------|--------------|-------|
| ST-1201 | VoiceMicButton component | 2 | P1 | None | - |
| ST-1202 | Audio recording with MediaRecorder | 3 | P1 | None | security_sensitive |
| ST-1203 | ASR upload + polling hook | 5 | P1 | None | - |
| ST-1204 | VoiceRecordingStates UI | 2 | P1 | None | - |
| ST-1205 | Integration with CommandInput | 5 | P1 | ST-1201, ST-1202, ST-1203, ST-1204 | diagrams_needed:lite |

**Total committed:** 17 points

**Deliverables:**
- VoiceMicButton component with states (idle, recording, processing, disabled)
- useAudioRecorder hook (start/stop, WebM/Opus output, 60s max, permission handling)
- useAsrTranscription hook (upload, poll with pollAfterMs, max 30 attempts)
- VoiceRecordingStatus component (timer, cancel button, state indicators)
- CommandInput integration (mic button, wire hooks, transcript to input)

---

### Out of Scope (explicit)

| Item | Reason | Deferred To |
|------|--------|-------------|
| Error handling UX (ST-1206) | Next sprint | S16 |
| Client telemetry events (ST-1207) | Next sprint | S16 |
| Cross-browser + accessibility polish (ST-1208) | Next sprint | S16 |
| Wake word / hands-free | Out of initiative scope | LATER |
| Real-time transcription | Initiative decision: async model | OUT |
| Voice notes as entity | Separate initiative | LATER |
| Offline transcription | Out of scope | OUT |

---

## Capacity Note

**Assumptions:**
- 1 developer (Codex)
- ST-1201: 1 day (component + tests)
- ST-1202: 1-2 days (hook + MediaRecorder mocking)
- ST-1203: 2 days (upload + polling + MSW tests)
- ST-1204: 1 day (status UI + timer)
- ST-1205: 2 days (integration + state management)
- Total: ~7-9 days for committed scope

**Constraints:**
- ST-1201, ST-1202, ST-1203, ST-1204 can run in parallel
- ST-1205 depends on all four (integration story)
- Recommended order: ST-1201 || ST-1204 first, then ST-1202 || ST-1203, finally ST-1205

**Buffer:** 15% (~3 points) for:
- MediaRecorder browser quirks
- Polling timing edge cases
- State management complexity in integration

---

## Assumptions

1. S14 completed successfully (EP-011 ASR Foundation done)
2. ASR proxy endpoints working and tested
3. MediaRecorder API available in target browsers
4. Existing CommandInput component can be extended
5. MSW (Mock Service Worker) available for hook testing

---

## Dependencies

| Dependency | Owner | Status | Risk |
|------------|-------|--------|------|
| EP-011 ASR Foundation (S13, S14) | Previous sprints | DONE | NONE |
| ASR Proxy Contract | Backend | DONE | NONE |
| CommandInput.tsx | Web codebase | EXISTS | LOW |
| MediaRecorder API | Browsers | SUPPORTED | LOW |
| MSW for tests | Web test infra | SET UP | LOW |

**No blocking dependencies.** All S15 stories are unblocked.

---

## Risks & Mitigations (ROAM-lite)

| Risk | Impact | Probability | Strategy | Notes |
|------|--------|-------------|----------|-------|
| MediaRecorder browser differences | MEDIUM | MEDIUM | Accept | Chrome primary, others tested in S16 |
| Microphone permission UX confusion | LOW | MEDIUM | Mitigate | Clear UI states, fallback to text |
| Polling timing issues | LOW | LOW | Mitigate | Use pollAfterMs from response |
| State management complexity | MEDIUM | LOW | Mitigate | Follow epic state machine diagram |
| Audio format incompatibility | LOW | LOW | Resolve | WebM/Opus validated in EP-011 tests |

---

## Definition of Ready Check

**DoR Status:** PASS

| Story | DoR | Notes |
|-------|-----|-------|
| ST-1201 | PASS | AC defined, states clear, test strategy ready |
| ST-1202 | PASS | AC defined, hook interface specified |
| ST-1203 | PASS | AC defined, contract exists, MSW tests |
| ST-1204 | PASS | AC defined, timer format specified |
| ST-1205 | PASS | AC defined, dependencies explicit |

**All prerequisites:**
- [x] ASR proxy complete (EP-011)
- [x] All stories have AC with testable conditions
- [x] Test strategies defined for each story
- [x] Flags identified (security_sensitive, diagrams_needed)
- [x] No blocking external dependencies

---

## Gate B Ask

**Request:** Approve Sprint S15 goal, committed scope (ST-1201 through ST-1205), and capacity note.

**What we commit to:**
1. VoiceMicButton with idle/recording/processing/disabled states
2. useAudioRecorder hook with start/stop/duration/error handling
3. useAsrTranscription hook with upload/poll/transcript/reset
4. VoiceRecordingStatus UI with timer and cancel
5. CommandInput integration: mic button visible, recording works, transcript populates input
6. Unit tests for each component/hook
7. Integration tests with MSW

**What we will NOT do:**
- Detailed error handling UX (S16)
- Client telemetry (S16)
- Cross-browser polish (S16)
- Accessibility hardening (S16)

**Approval needed:**
- [ ] Sprint goal approved
- [ ] Committed scope (17 points) approved
- [ ] Out of scope explicit list approved
- [ ] Risks accepted

---

## Sprint Artifacts

| Artifact | Path |
|----------|------|
| Sprint Plan | `docs/planning/pi/2026Q1-PI01/sprints/S15/sprint.md` (this file) |
| Scope Detail | `docs/planning/pi/2026Q1-PI01/sprints/S15/scope.md` |
| Demo Plan | `docs/planning/pi/2026Q1-PI01/sprints/S15/demo.md` |
| Retro | `docs/planning/pi/2026Q1-PI01/sprints/S15/retro.md` |

---

## Exit Criteria

Sprint is **done** when:
1. All committed stories completed (AC verified, DoD met)
2. VoiceMicButton renders all states correctly (ST-1201)
3. Recording produces WebM blob, 60s max enforced (ST-1202)
4. Permission denied handled gracefully (ST-1202)
5. ASR upload returns transcriptionId (ST-1203)
6. Polling completes with transcript (ST-1203)
7. Timer displays mm:ss format (ST-1204)
8. Cancel discards recording (ST-1204)
9. Mic button visible in CommandInput (ST-1205)
10. Transcript populates input field (ST-1205)
11. Submit command works after voice input (ST-1205)
12. `npm run test` passes (web)
13. Demo prepared

**Sprint fails if:**
- Recording does not work in Chrome
- Transcript does not populate input
- Submit fails after voice input
- Unit tests fail

---

## Epic Progress Note

After S15:
- **Milestone M1: Core flow works** achieved
- **EP-012 progress:** 5/8 stories complete
- Remaining: ST-1206, ST-1207, ST-1208 (S16 - hardening)
