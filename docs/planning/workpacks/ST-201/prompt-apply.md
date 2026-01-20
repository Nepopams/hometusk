# ST-201 APPLY Prompt

**Mode:** IMPLEMENTATION — Execute approved plan

---

## Context

You are implementing ST-201: Web Foundation (Project Setup & Build) for the HomeTusk web client.

**Read these files (mandatory):**
- `docs/planning/workpacks/ST-201/workpack.md` — implementation plan
- `docs/planning/epics/EP-003/stories/ST-201-web-foundation.md` — story spec
- Your approved PLAN output (if available)

---

## Your Task

Implement the web client foundation according to the workpack.

**Deliverables:**
1. Initialize project at `clients/web/`
2. Install dependencies (React, TypeScript, Vite, etc.)
3. Create all source files
4. Verify with commands

---

## Allowed Operations

### Directories to create
- `clients/web/**` (entire directory tree)

### Files to create
- `clients/web/package.json`
- `clients/web/tsconfig.json`
- `clients/web/tsconfig.node.json` (for Vite config)
- `clients/web/vite.config.ts`
- `clients/web/.eslintrc.cjs`
- `clients/web/.prettierrc`
- `clients/web/.env.example`
- `clients/web/.gitignore`
- `clients/web/index.html`
- `clients/web/README.md`
- `clients/web/src/vite-env.d.ts` (Vite types)
- `clients/web/src/**/*.tsx`
- `clients/web/src/**/*.ts`
- `clients/web/src/**/*.css`

### Commands allowed
- `mkdir -p clients/web/...`
- `npm init -y` (in clients/web)
- `npm install ...`
- `npm run dev` (to verify)
- `npm run build` (to verify)
- `npm run lint` (to verify)

### Forbidden
- **DO NOT modify** any existing files outside `clients/web/`
- **DO NOT modify** backend code
- **DO NOT create** test files (defer to later)

---

## Implementation Sequence

### Step 1: Create directory and initialize npm
```bash
mkdir -p clients/web
cd clients/web
npm init -y
```

### Step 2: Install dependencies
```bash
# Production
npm install react@18.2.0 react-dom@18.2.0 react-router-dom@6.16.0

# Development
npm install -D typescript@5.4.0 @types/react@18.2.0 @types/react-dom@18.2.0
npm install -D vite@5.0.0 @vitejs/plugin-react@4.0.0
npm install -D eslint@8.56.0 @typescript-eslint/eslint-plugin@7.0.0 @typescript-eslint/parser@7.0.0
npm install -D eslint-plugin-react@7.33.2 eslint-plugin-react-hooks@4.6.0 eslint-plugin-react-refresh@0.4.5
npm install -D prettier@3.2.5 eslint-config-prettier@9.1.0
```

### Step 3: Create config files
Create in order:
1. `tsconfig.json`
2. `tsconfig.node.json` (for Vite config)
3. `vite.config.ts`
4. `.eslintrc.cjs`
5. `.prettierrc`
6. `.gitignore`
7. `.env.example`
8. `index.html`

### Step 4: Create source files
Create directory structure:
```
src/
├── main.tsx
├── App.tsx
├── vite-env.d.ts (Vite environment types)
├── routes/
│   ├── index.tsx
│   ├── Login.tsx
│   ├── HouseholdLayout.tsx
│   ├── TasksList.tsx
│   ├── TaskDetail.tsx
│   ├── ZonesList.tsx
│   ├── Notifications.tsx
│   └── NotFound.tsx
├── components/
│   └── Layout/
│       ├── Layout.tsx
│       ├── Header.tsx
│       └── Sidebar.tsx
├── lib/
│   └── api.ts
└── styles/
    └── index.css
```

### Step 5: Update package.json scripts
```json
{
  "name": "hometusk-web",
  "private": true,
  "version": "0.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "preview": "vite preview",
    "lint": "eslint \"src/**/*.{ts,tsx}\" --max-warnings=0",
    "format": "prettier --write src"
  }
}
```

### Step 6: Create README.md

### Step 7: Verify
```bash
npm run lint   # must pass
npm run build  # must create dist/
npm run dev    # manual: dev server starts
```

---

## Key File Contents

### tsconfig.json
```json
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "skipLibCheck": true,
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "resolveJsonModule": true,
    "isolatedModules": true,
    "noEmit": true,
    "jsx": "react-jsx",
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true,
    "types": ["vite/client"]
  },
  "include": ["src"],
  "references": [{ "path": "./tsconfig.node.json" }]
}
```

### tsconfig.node.json
```json
{
  "compilerOptions": {
    "composite": true,
    "skipLibCheck": true,
    "module": "ESNext",
    "moduleResolution": "bundler",
    "allowSyntheticDefaultImports": true
  },
  "include": ["vite.config.ts"]
}
```

### src/vite-env.d.ts
```typescript
/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string;
  readonly VITE_AUTH_PROVIDER: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
```

### .gitignore
```
# dependencies
node_modules

# production build
dist

# local env files
.env
.env.local

# editor
.vscode
.idea
*.swp
*.swo

# OS
.DS_Store
Thumbs.db

# logs
*.log
npm-debug.log*
```

### vite.config.ts
```typescript
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
  },
});
```

### .eslintrc.cjs
```javascript
module.exports = {
  root: true,
  env: { browser: true, es2022: true },
  parser: '@typescript-eslint/parser',
  plugins: ['@typescript-eslint', 'react', 'react-hooks', 'react-refresh'],
  extends: [
    'eslint:recommended',
    'plugin:@typescript-eslint/recommended',
    'plugin:react/recommended',
    'plugin:react-hooks/recommended',
    'plugin:react-refresh/recommended',
    'prettier',
  ],
  settings: {
    react: { version: 'detect' },
  },
  rules: {
    'react/react-in-jsx-scope': 'off', // React 17+ doesn't need import React
  },
};
```

### .prettierrc
```json
{
  "singleQuote": true,
  "trailingComma": "all",
  "printWidth": 100
}
```

### Route structure (src/routes/index.tsx)
```typescript
import { createBrowserRouter, Navigate } from 'react-router-dom';
import Login from './Login';
import HouseholdLayout from './HouseholdLayout';
import TasksList from './TasksList';
import TaskDetail from './TaskDetail';
import ZonesList from './ZonesList';
import Notifications from './Notifications';
import NotFound from './NotFound';

export const router = createBrowserRouter([
  { path: '/', element: <Navigate to="/login" replace /> },
  { path: '/login', element: <Login /> },
  {
    path: '/households/:householdId',
    element: <HouseholdLayout />,
    children: [
      { index: true, element: <Navigate to="tasks" replace /> },
      { path: 'tasks', element: <TasksList /> },
      { path: 'tasks/:taskId', element: <TaskDetail /> },
      { path: 'zones', element: <ZonesList /> },
      { path: 'notifications', element: <Notifications /> },
    ],
  },
  { path: '*', element: <NotFound /> },
]);
```

---

## Acceptance Criteria Verification

After implementation, verify:
- [ ] AC1: `clients/web/package.json` exists with correct deps
- [ ] AC2: `npm run dev/build/lint` all work
- [ ] AC3: All routes work (manually test navigation)
- [ ] AC4: Layout visible with header/sidebar
- [ ] AC5: `.env.example` contains required vars
- [ ] AC6: `README.md` complete

---

## STOP-THE-LINE

If you encounter:
- npm install errors
- TypeScript config issues
- Vite build failures

**STOP and report** — do not proceed with workarounds.

---

## Report Format

After completion:
```markdown
# ST-201 Implementation Report

## Files Created
- [list all files]

## Commands Run
- [list all commands with results]

## Verification
- npm ci: PASS/FAIL
- npm run build: PASS/FAIL
- npm run lint: PASS/FAIL
- npm run dev: PASS/FAIL (manual check)

## Issues Encountered
- [any issues and how resolved]

## AC Status
- AC1: PASS/FAIL
- AC2: PASS/FAIL
- AC3: PASS/FAIL
- AC4: PASS/FAIL
- AC5: PASS/FAIL
- AC6: PASS/FAIL
```
