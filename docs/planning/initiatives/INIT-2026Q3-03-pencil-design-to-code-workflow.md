# INIT-2026Q3-03: Pencil Design-to-Code Workflow

## Status

Proposed

## Owner

HomeTusk frontend/design team.

## Problem

The Voice Command Chat design is currently represented in Pencil frames and PNG exports. If frontend implementation uses PNG screenshots only, Codex/Claude may approximate the design visually but miss structure, tokens, responsive rules, and component variants.

## Goal

Use Pencil as structured design source for implementation and QA.

Preferred source:

- `.pen` file in the repository;
- Pencil MCP available to Codex/Claude;
- PNG exports as visual fallback;
- markdown handoff documents as acceptance contract.

## Repository structure

```text
docs/design/voice-command-chat/
  voice-command-chat.pen
  exports/
    voice-chat-desktop-empty.png
    voice-chat-desktop-recording.png
    voice-chat-desktop-transcribing.png
    voice-chat-desktop-draft-ready.png
    voice-chat-desktop-executed-shopping-items.png
    voice-chat-desktop-executed-task-shopping-link.png
    voice-chat-desktop-needs-input.png
    voice-chat-desktop-rejected.png
    voice-chat-desktop-asr-permission-error.png
    voice-chat-desktop-asr-timeout-rate-limit.png
    voice-chat-mobile-empty.png
    voice-chat-mobile-recording-bottom-sheet.png
    voice-chat-mobile-transcribing.png
    voice-chat-mobile-draft-ready.png
    voice-chat-mobile-processing.png
    voice-chat-mobile-executed-shopping-items.png
    voice-chat-mobile-needs-input.png
    voice-chat-mobile-asr-permission-error.png
    voice-command-chat-components.png
    voice-command-chat-mvp-handoff-qa.png
  voice-command-chat-handoff.md
  implementation-map.md
  component-inventory.md
  responsive-rules.md
  frontend-acceptance-criteria.md
```

## MCP validation

Before implementation, verify:

```text
/mcp
```

Expected:

- `pencil` server is listed;
- server is enabled;
- authentication not supported is acceptable for local Pencil MCP.

## Design-to-code flow

### Phase 1: Design inventory

Codex inspects:

- `.pen` via Pencil MCP;
- PNG exports;
- existing frontend tokens/components.

Output:

- component inventory;
- responsive rules;
- mapping to existing components;
- design gaps.

### Phase 2: Implementation map

Codex maps design frames to frontend files.

Output:

- component tree;
- state machine;
- API touchpoints;
- routing point;
- test plan.

### Phase 3: UI skeleton

Implement components and static states.

No ASR and no command backend calls yet.

### Phase 4: ASR integration

Connect voice recording to HomeTusk ASR BFF.

### Phase 5: Command integration

Connect editable transcript to existing command API.

### Phase 6: Visual QA

Compare implementation with Pencil frames and PNG exports.

## Rules

- Use existing HomeTusk design tokens.
- Do not create a new visual language.
- Do not add a new navigation item.
- Do not make the product feel like a generic AI chatbot.
- Keep components simple and local unless reused elsewhere.
- Use `.pen` as source of truth where MCP is available.
- Use PNG exports only as visual fallback and QA references.

## Acceptance criteria

- `.pen` file is committed or clearly referenced.
- PNG exports are available in repository.
- Handoff docs exist.
- Frontend implementation references handoff docs in plan.
- Visual QA doc is created before merge.
- Deviations from Pencil are listed explicitly.

## Risks

### Risk: Codex sees PNG but not structural design

Mitigation: require `.pen` + MCP before implementation where possible.

### Risk: MCP unavailable in CI or remote agent

Mitigation: keep PNG exports and markdown handoff in repo.

### Risk: design tokens drift

Mitigation: implementation map must map Pencil colors/spacing to existing frontend tokens.

### Risk: overfitting pixel-perfect screenshots

Mitigation: prioritize component consistency and responsive behavior over exact pixel matching.
