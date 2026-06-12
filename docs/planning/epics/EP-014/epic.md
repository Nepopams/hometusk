# Epic: EP-014 — Mobile Navigation Collapse

## Sources of Truth
- Initiative: `docs/planning/initiatives/INIT-2026Q1-mobile-nav-collapse.md`
- Product Goal: `docs/planning/strategy/product-goal.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**COMPLETE** — 1/1 story delivered on 2026-06-12.

## Initiative Alignment
This epic implements `INIT-2026Q1-mobile-nav-collapse`:
- keep desktop navigation unchanged;
- move mobile navigation out of the primary content flow;
- expose navigation through a compact header control on phone-sized screens.

**Product Goal Pillar:** Analytics-first Web / Reliability. The change improves the daily web path without expanding backend scope.

---

## Epic Goal
On mobile screens, users should see household content immediately after the header and open navigation only when they need it.

---

## Non-Goals

| Item | Reason |
|------|--------|
| Backend/API changes | Initiative explicitly excludes contract and domain changes |
| Auth changes | Out of scope |
| Full page redesign | Keep diff minimal and focused |
| New design system | Existing shell styles are sufficient |
| PWA/mobile app work | Separate roadmap item |

---

## Stories

| ID | Title | Priority | Status |
|----|-------|----------|--------|
| [ST-1401](./stories/ST-1401-mobile-nav-collapse.md) | Collapse household navigation on mobile | P0 | DONE |

---

## Exit Criteria

1. Desktop layout keeps the left sidebar.
2. Mobile layout does not render the navigation as a large block before content.
3. Mobile header exposes a visible compact navigation control.
4. All existing navigation items remain available on mobile.
5. Mobile menu can close by toggle, outside click, route selection, and Escape.
6. Task content is visible near the first mobile viewport after the header.
7. No horizontal scroll is introduced.
8. Backend contracts/API remain untouched.

---

## Flags Summary

| Flag | Value |
|------|-------|
| contract_impact | no |
| data_impact | no |
| adr_needed | none |
| diagrams_needed | no |
| security_sensitive | no |
| traceability_critical | no |
