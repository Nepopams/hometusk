# INIT-2026Q3-01: Voice Command Chat MVP

## Status

Proposed

## Owner

HomeTusk product/backend/frontend team.

## Problem

HomeTusk already supports a command-driven household workflow, but command input is still primarily typed. The product direction requires a faster natural interface: a user should be able to speak a household command, review the transcript, and run the existing command pipeline.

## Goal

Implement a voice-to-command experience inside the existing Commands area.

The user flow is:

1. User opens Commands.
2. User taps microphone.
3. User records a short voice command.
4. HomeTusk sends audio for ASR.
5. ASR returns transcript draft.
6. User reviews/edits the transcript.
7. User manually sends the command.
8. HomeTusk runs the existing command pipeline.
9. UI renders one of:
   - executed result,
   - needs input,
   - rejected,
   - controlled ASR error.

## Non-goals

- No generic assistant chat.
- No separate Voice/AI navigation item.
- No auto-send after ASR.
- No wake word.
- No TTS.
- No streaming ASR.
- No raw model/guardrail/schema/trace details in primary UI.
- No LLM logic inside HomeTusk.

## User scenarios

### Scenario 1: Add shopping items by voice

User says:

> add trash bags and radish to the Auchan shopping list

Expected result:

- transcript appears as editable draft;
- after manual send, command pipeline runs;
- shopping items are added;
- UI shows structured result card.

### Scenario 2: Create task and linked shopping items

User says:

> buy milk and chicken for dinner

Expected result:

- transcript appears as editable draft;
- after manual send, platform decision may create task and shopping items;
- HomeTusk applies schema validation and guardrails;
- UI shows task-shopping linkage.

### Scenario 3: Needs input

User says:

> clean the kitchen tomorrow evening

Expected result:

- command pipeline may return `needs_input`;
- UI presents a clarification card and chips/free-text answer;
- this is not shown as an error.

### Scenario 4: Rejected command

User says:

> clean the kitchen yesterday evening

Expected result:

- pipeline rejects unsafe/invalid command;
- UI shows recovery path: edit draft and send again.

## UX rules

- User must explicitly send the transcript.
- Transcript remains editable before command execution.
- ASR errors preserve existing typed/draft text when possible.
- `needs_input` is a clarification state.
- `rejected` is a controlled business outcome.
- Result cards must show concrete domain changes.
- Voice flow must work on desktop, tablet, and mobile.

## Acceptance criteria

### Functional

- User can record voice from Commands screen.
- User can cancel recording.
- User can retry voice after permission or ASR error.
- Transcript appears in existing command input as editable text.
- Send action invokes the existing Commands API.
- Command result is rendered as structured chat/result cards.
- Existing typed command path still works.

### Product

- No new navigation entry.
- Copy is calm and productivity-oriented.
- UI does not present itself as a generic chatbot.

### Safety

- No command execution before user confirmation.
- No raw audio persistence.
- No raw transcript logging.
- No platform internals in primary UI.

### Responsive

- Desktop uses Commands shell with main chat column.
- Tablet uses a readable single-column variant.
- Mobile uses sticky bottom input and recording bottom sheet.

## Dependencies

- AI Platform ASR endpoint.
- HomeTusk ASR BFF/proxy endpoint.
- Existing `/api/v1/commands` flow.
- Pencil design package.

## Risks

### Risk: ASR transcript mistakes

Mitigation: no auto-send; transcript is editable.

### Risk: UI becomes generic assistant chat

Mitigation: keep flow inside Commands and render domain result cards.

### Risk: design drift from existing HomeTusk UI

Mitigation: use Pencil `.pen` via MCP and PNG exports as QA references.

### Risk: incomplete command output mapping

Mitigation: render only known statuses first; fallback to controlled generic result card.

## Metrics

- voice starts per user/day;
- transcript success rate;
- ASR error rate by error type;
- transcript edit rate before send;
- command success/needs_input/rejected ratio;
- average ASR latency;
- average end-to-end command latency.
