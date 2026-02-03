# Story: ST-1206 — Error Handling UX

## Status: Ready

## Description
Graceful error handling for all voice input failures with fallback to text.

## In Scope
- `clients/web/src/components/commands/VoiceErrorMessage.tsx`
- Error messages: permission denied, too long, upload fail, ASR fail, rate limit
- "Try again" and "Type instead" actions
- Non-toxic messaging

## Acceptance Criteria
- AC-1: Permission denied shows helpful message + "Type instead"
- AC-2: Audio too long shows message + "Try again"
- AC-3: Upload failure shows message + both buttons
- AC-4: ASR failure shows message + both buttons
- AC-5: Rate limit shows retry countdown
- AC-6: "Try again" restarts voice flow
- AC-7: "Type instead" returns to text input
- AC-8: Non-toxic messaging (no blame)

## Points: 3

## Flags
- contract_impact: no
