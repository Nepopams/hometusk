# Voice Command Chat Frontend Acceptance Criteria

**Initiative:** INIT-2026Q3-voice-command-chat-mvp
**Date:** 2026-06-14

## Functional

- [ ] Commands screen shows a mic entry point when voice is enabled.
- [ ] User can start, stop, and cancel browser recording.
- [ ] Audio is uploaded only after recording stops.
- [ ] ASR success inserts transcript into the existing editable command textarea.
- [ ] Command is not sent until the user clicks Send/Run.
- [ ] Voice-originated command sends `source=voice` and `asrTraceId` when available.
- [ ] Typed command flow still sends `source=web`.
- [ ] `needs_input`, `rejected`, `executed`, `scheduled`, and degraded results render through existing structured cards.
- [ ] Permission denied, timeout, rate limit, unsupported media, and generic ASR failures are controlled UI states.
- [ ] ASR failure never calls `/api/v1/commands`.

## Responsive And Accessibility

- [ ] Desktop uses the existing two-column Commands shell.
- [ ] Mobile has no horizontal overflow and all voice controls remain tappable.
- [ ] Mic button has an accessible label and pressed state.
- [ ] Recording/transcribing state is announced to assistive tech.
- [ ] Error state is announced and offers retry or typed fallback.
- [ ] Escape cancels active recording.

## Privacy

- [ ] Frontend does not persist raw audio.
- [ ] Frontend telemetry stores only event type, correlation id, duration, and error type.
- [ ] Transcript text is not logged by voice telemetry.
