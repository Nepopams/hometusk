# Story: ST-1201 — VoiceMicButton Component

## Status: Ready

## Description
Create reusable VoiceMicButton component with states: idle, recording, processing, disabled.

## In Scope
- `clients/web/src/components/commands/VoiceMicButton.tsx`
- Visual states with icons and animations
- Accessible: aria-label, keyboard focusable

## Out of Scope
- Recording logic (ST-1202)
- Integration (ST-1205)

## Acceptance Criteria
- AC-1: Renders in idle state with mic icon
- AC-2: Renders in recording state with pulse animation
- AC-3: Renders in processing state with spinner
- AC-4: Disabled state greyed out
- AC-5: onClick fires on click
- AC-6: Keyboard accessible (Tab, Enter/Space)

## Test Strategy
- Unit tests for each state
- Keyboard interaction tests

## Points: 2

## Flags
- contract_impact: no
- adr_needed: no
- security_sensitive: no
