# Voice Command Chat Responsive Rules

**Initiative:** INIT-2026Q3-voice-command-chat-mvp
**Date:** 2026-06-14

## Shared Rules

- Keep Voice Command Chat inside the existing Commands layout.
- Preserve the current desktop history rail and mobile history sheet.
- Keep touch targets at least 44px for mic, cancel, retry, and send actions.
- Do not allow voice status text, timer, or buttons to resize the composer layout.
- The transcript draft is always editable before Send.
- Typed command behavior remains available when voice is disabled or fails.

## Desktop

- Use the existing two-column Commands shell.
- Place mic and ASR state inside the composer, directly attached to the textarea.
- Keep result cards below the composer in the left column.
- Keep history in the right rail.

## Tablet

- Allow the current layout breakpoint to collapse naturally when horizontal space is limited.
- Keep voice controls in the composer, not in the history rail.
- Avoid dense multi-control rows when width is constrained; wrap action buttons.

## Mobile

- Use the current single-column Commands page.
- Keep mic, recording/transcribing status, and errors above composer actions.
- Recording status may wrap to two rows; controls must remain reachable without horizontal scroll.
- Preserve draft text and attributes when ASR errors are dismissed.

## Accessibility

- Mic has `aria-pressed=true` while recording.
- Recording/transcribing status uses live regions.
- Controlled ASR errors use `role=alert`.
- Keyboard Escape cancels active recording.
- Send is disabled until editable command text is non-empty.
