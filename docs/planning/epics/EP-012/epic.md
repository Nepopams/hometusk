# Epic: EP-012 — Voice Input for Web Commands

## Sources of Truth
- Initiative: `docs/planning/initiatives/INIT-2026Q2-voice-input-web.md`
- ASR Proxy Contract: `docs/contracts/http/asr-proxy.openapi.yaml`
- Product Goal: `docs/planning/strategy/product-goal.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Approved** — Epic Gate approved 2026-02-03

## Initiative Alignment
This epic implements INIT-2026Q2-voice-input-web:
- Voice input as alternative to text command input
- Integration with existing ASR proxy (EP-011 complete)
- Graceful errors, editable transcript, fallback to text

**Product Goal Pillar:** Fairness & Transparency (lower barrier to task creation via voice)

---

## Epic Goal
Enable HomeTusk web users to:
1. **Record voice** via microphone button
2. **Send audio to ASR** via existing proxy endpoints
3. **See transcript** in editable text field
4. **Edit and submit** as normal command
5. **Handle errors gracefully**

---

## Non-Goals

| Item | Reason |
|------|--------|
| Wake word / hands-free | Out of scope for MVP |
| Real-time transcription | Async model sufficient |
| Voice notes as entity | Separate initiative |
| Offline transcription | Out of scope |

---

## Key Decisions

### A) Audio Format: WebM/Opus (MediaRecorder default)
### B) Recording Flow: Press-to-record / Press-to-stop
### C) State Machine: Linear states (idle → recording → uploading → transcribing → ready)
### D) Polling: Use `pollAfterMs` from ASR response

---

## Dependencies

| Dependency | Status |
|------------|--------|
| EP-011 ASR Foundation | **COMPLETE** |
| CommandInput.tsx | Exists |
| Browser MediaRecorder | Supported |

---

## Stories

| ID | Title | Priority | Sprint |
|----|-------|----------|--------|
| ST-1201 | VoiceMicButton component | P1 | S15 |
| ST-1202 | Audio recording with MediaRecorder | P1 | S15 |
| ST-1203 | ASR upload + polling hook | P1 | S15 |
| ST-1204 | VoiceRecordingStates UI | P1 | S15 |
| ST-1205 | Integration with CommandInput | P1 | S15 |
| ST-1206 | Error handling UX | P1 | S16 |
| ST-1207 | Client telemetry events | P2 | S16 |
| ST-1208 | Cross-browser + accessibility | P2 | S16 |

---

## Exit Criteria

1. User can record audio via mic button
2. Transcript appears in editable input field
3. User can submit command
4. Permission denied handled gracefully
5. ASR failure handled with fallback
6. Works in Chrome, Firefox, Safari, Edge
7. Keyboard accessible

---

## Flags

| Flag | Value |
|------|-------|
| contract_impact | no |
| adr_needed | no |
| diagrams_needed | lite |
| security_sensitive | yes |
