# HomeTusk Web Client

Web client for HomeTusk — desktop-first SPA with OIDC authentication support.

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

## Auth Setup

The app supports two authentication modes:

### Dev Mode (Token Paste)
For local development without Keycloak. Paste a valid JWT token directly.

```bash
# .env.local
VITE_AUTH_PROVIDER=dev
```

### Keycloak Mode (OIDC)
Full OIDC flow with Keycloak. Requires Keycloak to be running.

```bash
# .env.local
VITE_AUTH_PROVIDER=keycloak
VITE_OIDC_AUTHORITY=http://localhost:8180/realms/hometusk
VITE_OIDC_CLIENT_ID=hometusk-web
VITE_OIDC_REDIRECT_URI=http://localhost:5173/callback
```

### Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `VITE_API_BASE_URL` | Yes | — | Backend API URL |
| `VITE_AUTH_PROVIDER` | Yes | — | `dev` or `keycloak` |
| `VITE_OIDC_AUTHORITY` | Keycloak only | — | Keycloak realm URL |
| `VITE_OIDC_CLIENT_ID` | Keycloak only | — | Keycloak client ID |
| `VITE_OIDC_REDIRECT_URI` | Keycloak only | — | Callback URL |

### Auth Flow

1. User visits `/login`
2. **Dev mode**: Paste JWT token -> `/households`
3. **Keycloak mode**: Click "Sign in" -> Keycloak login -> `/callback` -> `/households`

For detailed Keycloak setup, see `docs/runbooks/local-dev.md`.

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
    ├── context/
    ├── hooks/
    ├── lib/
    │   └── auth/        # OIDC integration
    └── styles/
```
