# HomeTusk Web Client

Web foundation for the HomeTusk desktop-first client. This module provides routing, layout shell, and build tooling only. No backend integration yet.

## Requirements
- Node.js (LTS recommended)
- npm

## Setup
```bash
npm ci
```

## Run (dev)
```bash
npm run dev
```

## Build
```bash
npm run build
```

## Lint
```bash
npm run lint
```

## Environment variables
Create a `.env.local` file from the template if you want to override defaults.

```
VITE_API_BASE_URL=http://localhost:8080/api/v1
VITE_AUTH_PROVIDER=dev
```

## Folder structure
```
clients/web/
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
└── src/
    ├── main.tsx
    ├── App.tsx
    ├── routes/
    ├── components/
    ├── lib/
    └── styles/
```
