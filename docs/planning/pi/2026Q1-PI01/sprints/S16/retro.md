# Sprint S16 — Retrospective

## Date
2026-02-06 (end of sprint)

## Attendees
- Product Owner
- Claude Code (Architecture/BA)
- Codex (Development)

---

## Sprint Metrics

| Metric | Planned | Actual |
|--------|---------|--------|
| Committed Points | 24 | **24** |
| Completed Points | 24 | **27** |
| Stretch Completed | 0-3 | **3** |
| Stories Committed | 7 | **7** |
| Stories Completed | 7 | **8** (7+1 stretch) |
| Carry-over | 0 | **0** |

**Velocity: 27 points** (112% of committed scope)

---

## Completed Stories

| ID | Title | Points | Commit |
|----|-------|--------|--------|
| ST-1301 | ShoppingRun entity + repository | 5 | ✅ |
| ST-1303 | Export shopping list (text/CSV) | 3 | `3a61abb` |
| ST-1304 | Marketplace link-out templates | 5 | `f6481ec` |
| ST-1309 | Task-shopping navigation | 3 | `70992ea` |
| ST-1206 | Voice error handling UX | 3 | `735a542` |
| ST-1207 | Client telemetry events | 2 | `86603c9` |
| ST-1208 | Cross-browser + accessibility | 3 | `1133834` |
| ST-1310 | URL safe encoding guardrails | 3 | `321f848` (stretch) |

---

## What Went Well

1. **100% committed scope delivered** — все 7 committed stories завершены
2. **Stretch story ST-1310 included** — URL encoding guardrails реализованы с unit tests (vitest)
3. **Contract-first approach** — OpenAPI контракты создавались до имплементации, нет дрифта
4. **Clean pipeline** — Claude prompts → Codex PLAN → Codex APPLY → review workflow работает стабильно
5. **ADR-driven design** — ADR-014 (ShoppingRun) и ADR-015 (encoding) обеспечили ясность решений

---

## What Could Be Improved

1. **Pre-existing lint errors** — несколько файлов (AcceptInviteModal, CreateHouseholdModal) имели lint warnings; не блокирует, но требует cleanup
2. **ST-1206 AC-5 missed initially** — rate limit countdown не был реализован в первой итерации, потребовался patch prompt
3. **Vitest не был настроен** — для ST-1310 пришлось добавлять test framework в package.json

---

## Action Items

| Action | Owner | Due Date | Status |
|--------|-------|----------|--------|
| Fix pre-existing lint errors (S17 hygiene) | Dev | S17 | OPEN |
| Add unit test coverage check to CI | Dev | S17 | OPEN |
| Document workpack AC review checklist | BA | S17 | OPEN |

---

## Discussion Topics

### Process
- ✅ Sprint planning effectiveness: scope был точно рассчитан (24 + 3 stretch)
- ✅ Story readiness (DoR): все stories прошли DoR до спринта
- ✅ Communication: prompt-plan → findings → prompt-apply pipeline clear

### Technical
- ⚠️ Tech debt: lint warnings в старых файлах (minor)
- ✅ Architecture: ADR-014/015 не требуют изменений
- ✅ Testing: integration tests для всех backend endpoints, vitest для frontend

### Team
- ✅ Capacity accurate: 24 committed + 3 stretch = 27 delivered
- ✅ Blockers resolved quickly: ST-1206 patch handled in same session
- ✅ Collaboration effective: Claude/Codex handoff clean

---

## Carry-over Analysis

**No carry-over** — все committed stories завершены.

| Story | Reason | Action |
|-------|--------|--------|
| — | — | — |

---

## Notes

- EP-012 Voice Input **полностью завершён** (S15 core + S16 polish)
- EP-013 Shopping Marketplaces foundation ready for S17 UI work
- URL encoding (ST-1310) обеспечивает безопасность marketplace link-outs
- Shopping runs entity/repo готовы для REST endpoints в S17

### Technical Highlights
- Safari MediaRecorder support via audio/mp4 MIME fallback
- RFC 3986 URL encoding с XSS prevention
- localStorage ring buffer для telemetry (max 100 events)
- Task-shopping bidirectional navigation

---

## Sprint Goal Review

**Goal:** Establish Shopping Marketplaces backend foundation + complete Voice Input EP-012 polish

**Verdict: ✅ ACHIEVED**
- ShoppingRun entity/repo: DONE
- Export endpoint: DONE
- Marketplace templates: DONE
- Task-shopping navigation: DONE
- Voice error handling: DONE
- Client telemetry: DONE
- Cross-browser/a11y: DONE
- (Bonus) URL encoding: DONE
