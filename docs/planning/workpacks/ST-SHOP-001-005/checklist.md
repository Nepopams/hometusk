# ST-SHOP-001-005 Checklist

## Gate Decisions
- [x] Gate A/B: approved by delegated human authority for current NOW initiative.
- [x] Artifact gate: approved as non-breaking HTTP v1 additive delta.
- [x] Gate C: approved for APPLY within workpack file list.
- [x] Gate D: completed by delegated review gate; GO.

## Backend
- [x] Create shopping list endpoint implemented.
- [x] Create list validation: required trimmed name, 1-80 chars.
- [x] Add item accepts category/source/linkedTaskId.
- [x] Update item supports linkedTaskId and null unlink.
- [x] Cross-household linkedTaskId rejected for manual REST.
- [x] Command/AI add item safe fallback unchanged.
- [x] Backend integration tests pass.

## Web
- [x] Empty shopping list page has create CTA.
- [x] Non-empty shopping list page has create action.
- [x] Create list redirects to detail.
- [x] Add item form supports quantity/unit/category/source/task.
- [x] Edit item modal supports category/source/task link.
- [x] Task detail shows linked shopping items.
- [x] Task detail can add a linked shopping item.
- [x] Web build/tests pass.

## Docs
- [x] OpenAPI updated.
- [x] API coverage updated.
- [x] Service catalog updated.
- [x] ADR-009 note updated.
- [x] Contract index note updated.
- [x] Initiative/roadmap closure updated.

## Review
- [x] Review gate completed.
- [x] No Must-fix findings remain.
