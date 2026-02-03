# Sprint S15 — Demo Plan

## Sources of Truth
- Sprint Plan: `docs/planning/pi/2026Q1-PI01/sprints/S15/sprint.md`
- Epic: `docs/planning/epics/EP-012/epic.md`

---

## Demo Goal
Demonstrate core voice input flow: user records voice, sees transcript, edits if needed, submits command.

**Audience:** Product Owner, Stakeholders
**Duration:** ~10-15 minutes

---

## Demo Scenarios

### 1. VoiceMicButton States (ST-1201)

#### 1.1 Idle State
- Open web app, navigate to command input
- Observe mic button in idle state (mic icon, neutral color)
- Hover shows tooltip: "Start voice recording"

#### 1.2 Visual States Preview
- (Component Storybook or manual toggle)
- Show idle -> recording (pulse animation) -> processing (spinner) -> disabled (greyed)

---

### 2. Audio Recording (ST-1202)

#### 2.1 Permission Request
- Click mic button
- Browser shows permission dialog
- Grant permission
- Recording starts (mic button shows recording state)

#### 2.2 Recording with Timer
- Speak for ~10 seconds
- Observe timer counting (00:00 -> 00:10)
- Click mic again to stop

#### 2.3 Auto-Stop at 60s
- Start recording
- Wait (or fast-forward demo) to 60 seconds
- Recording auto-stops

#### 2.4 Permission Denied (stretch)
- Reset permission in browser settings
- Click mic button
- Deny permission
- Observe error state / fallback to text

---

### 3. ASR Upload and Polling (ST-1203)

#### 3.1 Upload Flow
- After recording stops, observe "Uploading..." state
- Upload completes, transitions to "Transcribing..."

#### 3.2 Polling Completion
- Wait for ASR to complete
- Transcript appears in input field
- Show in dev tools: network tab with polling requests

#### 3.3 Headers Verification
- Open network tab
- Show POST request with:
  - Idempotency-Key header
  - X-Correlation-ID header
  - Authorization header

---

### 4. Recording States UI (ST-1204)

#### 4.1 Timer Display
- During recording, show timer incrementing
- Format: "00:45" for 45 seconds, "01:15" for 75 seconds

#### 4.2 Cancel Button
- Start recording
- Click "Cancel" button
- Recording discarded, return to idle state

#### 4.3 Processing States
- Show uploading indicator (spinner + "Uploading...")
- Show transcribing indicator (spinner + "Transcribing...")

---

### 5. Full Integration Flow (ST-1205)

#### 5.1 Happy Path E2E
```
1. Open HomeTusk web app
2. Navigate to command input (e.g., Tasks page)
3. See mic button next to text input
4. Click mic button
5. Grant permission if prompted
6. Say: "Clean the kitchen tomorrow at 5pm"
7. Click mic to stop recording
8. See "Uploading..." then "Transcribing..."
9. See transcript appear in input field: "Clean the kitchen tomorrow at 5pm"
10. Edit if needed (e.g., add "please")
11. Click Submit
12. Command executes, task created
```

#### 5.2 Edit Before Submit
- Complete voice flow
- Transcript appears in input
- Manually edit text (change "kitchen" to "bathroom")
- Submit edited command
- Verify command uses edited text

#### 5.3 Cancel Mid-Recording
- Click mic to start recording
- Speak for a few seconds
- Click "Cancel"
- Recording discarded, input cleared, back to idle

#### 5.4 Submit Normal Text After Voice Cancel
- Cancel a voice recording
- Type text manually in input
- Submit text command
- Verify works as before (regression check)

---

## Success Criteria

| Scenario | Expected | Status |
|----------|----------|--------|
| Mic button visible | Shows in CommandInput | [ ] |
| Idle state | Mic icon, clickable | [ ] |
| Recording state | Pulse animation visible | [ ] |
| Processing state | Spinner visible | [ ] |
| Permission request | Browser dialog appears | [ ] |
| Recording produces audio | WebM blob created | [ ] |
| Timer shows duration | mm:ss format | [ ] |
| Auto-stop at 60s | Recording stops automatically | [ ] |
| Upload starts | "Uploading..." shown | [ ] |
| Polling works | "Transcribing..." shown | [ ] |
| Transcript appears | Text in input field | [ ] |
| Transcript editable | Can modify text | [ ] |
| Submit works | Command executes | [ ] |
| Cancel discards | Returns to idle | [ ] |
| Headers present | Idempotency-Key, X-Correlation-ID | [ ] |

---

## Demo Environment

**Requirements:**
- Web app running locally (or staging)
- User logged in with household
- Working microphone
- ASR proxy running (or mocked with WireMock)
- Browser: Chrome (primary)

**Pre-demo checklist:**
- [ ] Web app started (`npm run dev`)
- [ ] Backend started (or mock)
- [ ] ASR proxy available (or WireMock stubs)
- [ ] Microphone connected and tested
- [ ] Browser permissions reset (for permission demo)
- [ ] Test household with tasks ready

---

## Demo Script

### Opening (1 min)
"Today we're demonstrating voice input for commands. Users can now speak commands instead of typing, making task creation faster and more accessible."

### Mic Button Demo (2 min)
"Here's the new mic button in the command input. Let me show the different states..."

### Recording Demo (3 min)
"Watch as I record a voice command. Notice the timer and the option to cancel..."

### Transcription Demo (2 min)
"The audio is uploaded to our ASR service. You can see the polling in the network tab. And here's the transcript..."

### Edit and Submit Demo (2 min)
"The transcript is editable - I can fix any recognition errors before submitting. Now let me submit..."

### Cancel Flow Demo (2 min)
"If I change my mind, I can cancel at any time and fall back to text input..."

### Q&A (2-3 min)
"Questions?"

---

## Fallback Plan

If ASR service unavailable:
- Use WireMock stubs with pre-recorded responses
- Demo shows same UI flow with mocked backend

If microphone issues:
- Pre-record a demo video
- Show component states manually via props

---

## Notes

- Primary browser: Chrome (Firefox/Safari are S16)
- Error handling polish is S16 scope
- Focus demo on happy path, mention error handling as "coming next sprint"
- Have network tab open to show API calls
