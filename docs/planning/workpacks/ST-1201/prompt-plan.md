# Codex PLAN Prompt: ST-1201 — VoiceMicButton Component

## Mission
You are in **PLAN mode** (read-only exploration). Your goal is to understand the codebase structure and prepare findings for the APPLY phase.

**DO NOT** edit, create, or modify any files. Only read and analyze.

---

## Context
Creating a `VoiceMicButton` React component for voice input feature in HomeTusk web client.

### Story Reference
- Story: `docs/planning/epics/EP-012/stories/ST-1201-voice-mic-button.md`
- Workpack: `docs/planning/workpacks/ST-1201/workpack.md`

### Component Requirements
- 4 visual states: idle, recording, processing, disabled
- Mic icon (SVG)
- Red pulsing animation for recording
- Spinner for processing
- Accessibility: aria-label, aria-pressed, focus-visible

---

## Exploration Tasks

### 1. Web Client Structure
```bash
# Find the web client location
ls -la clients/web/src/

# Check components structure
ls -la clients/web/src/components/

# Check if commands folder exists
ls -la clients/web/src/components/commands/ 2>/dev/null || echo "commands folder does not exist"
```

### 2. Existing Component Patterns
```bash
# Find existing button components for pattern reference
find clients/web/src -name "*.tsx" -type f | head -20

# Check existing component structure (pick one to examine pattern)
cat clients/web/src/components/**/*Button*.tsx 2>/dev/null | head -50

# Or check any existing component
ls clients/web/src/components/
```

### 3. CSS Approach
```bash
# Check how CSS is organized
find clients/web/src -name "*.css" -type f | head -10

# Check if CSS modules or plain CSS
ls clients/web/src/components/**/*.css 2>/dev/null | head -5

# Check for CSS variables / theme
find clients/web/src -name "*.css" -exec grep -l "var(--" {} \; | head -5
```

### 4. TypeScript Config
```bash
# Check TypeScript config
cat clients/web/tsconfig.json

# Check if strict mode enabled
grep -E "strict|noImplicit" clients/web/tsconfig.json
```

### 5. Test Setup
```bash
# Check test setup
ls clients/web/src/**/*.test.tsx 2>/dev/null | head -5

# Check test config
cat clients/web/jest.config.* 2>/dev/null || cat clients/web/vitest.config.* 2>/dev/null

# Check testing library used
grep -E "jest|vitest|testing-library" clients/web/package.json
```

### 6. Export Patterns
```bash
# Check if index.ts exists for exports
cat clients/web/src/components/index.ts 2>/dev/null
cat clients/web/src/components/commands/index.ts 2>/dev/null

# Check barrel export pattern used
find clients/web/src -name "index.ts" -type f | head -10
```

### 7. Package Dependencies
```bash
# Check package.json for relevant deps
cat clients/web/package.json | grep -E "react|testing|css"
```

---

## Expected Findings to Report

Please provide findings on:

1. **File locations**:
   - Where to create `VoiceMicButton.tsx`
   - Where to create `VoiceMicButton.css`
   - Whether commands folder exists or needs creation

2. **Patterns to follow**:
   - Component file structure pattern
   - CSS approach (modules, plain, styled-components)
   - Export pattern (barrel exports?)
   - TypeScript conventions

3. **Test setup**:
   - Testing library (Jest/Vitest + React Testing Library?)
   - Test file naming convention
   - Test location

4. **CSS variables**:
   - Existing color variables to use
   - Or fallback values needed

5. **Blockers or clarifications**:
   - Any missing dependencies
   - Any conflicts with existing code

---

## Constraints (PLAN mode)

**ALLOWED commands:**
- `ls`, `find`
- `cat`, `head`, `tail`
- `grep`, `rg`
- `git status`, `git diff` (read-only)

**FORBIDDEN:**
- Any file edits/writes
- `npm install`, `npm run`
- Creating new files
- Git commits

---

## Output Format

Structure your findings as:

```
## PLAN Findings: ST-1201

### 1. File Locations
- Component: clients/web/src/components/commands/VoiceMicButton.tsx
- Styles: clients/web/src/components/commands/VoiceMicButton.css
- Commands folder: [exists/needs creation]

### 2. Patterns Observed
- Component structure: [describe]
- CSS approach: [plain/modules/styled]
- Exports: [barrel/direct]

### 3. Test Setup
- Library: [Jest/Vitest]
- Convention: [*.test.tsx / *.spec.tsx]
- Location: [same folder / __tests__]

### 4. CSS Variables
- Available: [list or "none found"]
- Recommend: [use fallbacks]

### 5. Blockers
- [list or "none"]

### 6. Ready for APPLY
- [Yes/No with reason]
```
