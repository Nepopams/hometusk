# Story: ST-1208 — Cross-Browser Polish + Accessibility

## Status: Ready

## Description
Polish for cross-browser compatibility and accessibility.

## In Scope
- Test matrix: Chrome, Firefox, Safari, Edge (latest)
- Browser-specific MediaRecorder fixes
- Keyboard navigation (Tab, Enter, Space, Escape)
- ARIA attributes, focus management
- Visual focus indicators

## Out of Scope
- Legacy browsers (IE, old Safari)
- Mobile browsers

## Acceptance Criteria
- AC-1: Works in Chrome (latest)
- AC-2: Works in Firefox (latest)
- AC-3: Works in Safari (latest) or graceful degradation
- AC-4: Works in Edge (latest)
- AC-5: Full keyboard navigation
- AC-6: Screen reader announces states
- AC-7: Focus moves to input when transcript ready
- AC-8: Mic hidden if MediaRecorder unsupported

## Test Strategy
- Manual testing in all browsers
- Lighthouse accessibility score >= 90
- axe-core zero violations

## Points: 3

## Flags
- security_sensitive: yes (browser permissions)
