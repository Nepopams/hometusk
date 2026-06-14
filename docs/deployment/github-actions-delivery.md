# GitHub Actions delivery model

**Status:** initial UAT delivery setup  
**Date:** 2026-06-12

This document defines the lightweight delivery model for HomeTusk. The goal is to avoid a Jenkins-like platform until there is a real need for it.

## Branch model

| Branch | Purpose | Deployment |
|---|---|---|
| `develop` | Integration line for active development | auto-deploy to `dev` |
| `main` | Production-candidate line | auto-deploy to `uat` |
| feature branches | Short-lived implementation branches | no deployment |

Production is not represented by a long-lived branch. Production deploys are manual promotions of a known immutable image tag that has already been verified on UAT.

## Workflows

| Workflow | Trigger | Target |
|---|---|---|
| `CI` | push/PR to `main` or `develop` | checks only |
| `Deploy Dev` | push to `develop`, manual | `dev` |
| `Deploy UAT` | push to `main`, manual | `uat` |
| `Deploy Prod` | manual only | `prod` |

## Image model

GitHub Actions builds deployable backend, web, and Keycloak images and pushes
them to GHCR:

```text
ghcr.io/<owner>/hometusk-backend:sha-<commit_sha>
ghcr.io/<owner>/hometusk-web:sha-<commit_sha>
ghcr.io/<owner>/hometusk-keycloak:sha-<commit_sha>
```

Convenience tags are also maintained:

```text
ghcr.io/<owner>/hometusk-backend:develop-latest
ghcr.io/<owner>/hometusk-web:develop-latest
ghcr.io/<owner>/hometusk-keycloak:develop-latest
ghcr.io/<owner>/hometusk-backend:main-latest
ghcr.io/<owner>/hometusk-web:main-latest
ghcr.io/<owner>/hometusk-keycloak:main-latest
```

Do not use `*-latest` for production. Production should use the immutable `sha-*` tag.

## Required GitHub environments

Create these environments in GitHub repository settings:

```text
dev
uat
prod
```

Recommended protection:

| Environment | Protection |
|---|---|
| `dev` | none |
| `uat` | optional |
| `prod` | required reviewer |

## Required secrets

### Dev

```text
DEV_HOST
DEV_USER
DEV_SSH_KEY
DEV_DEPLOY_PATH
DEV_ENV_FILE
DEV_OIDC_AUTHORITY
DEV_OIDC_REDIRECT_URI
```

### UAT

```text
UAT_HOST
UAT_USER
UAT_SSH_KEY
UAT_DEPLOY_PATH
UAT_ENV_FILE
UAT_OIDC_AUTHORITY
UAT_OIDC_REDIRECT_URI
```

The browser image is always built with `VITE_OIDC_CLIENT_ID=hometusk-web`.
Do not point the SPA at `hometusk-api`; that legacy client is not the canonical
browser client and can bypass the social-auth broker redirect guardrails.
The UAT workflow sets `EXPECT_YANDEX_IDP=true`, so a green UAT deploy also
requires the Keycloak social IdP configurator to complete successfully and the
public `kc_idp_hint=yandex` authorization request to redirect to broker/Yandex.

### Prod

```text
PROD_HOST
PROD_USER
PROD_SSH_KEY
PROD_DEPLOY_PATH
PROD_ENV_FILE
```

Optional shared secret:

```text
GHCR_READ_TOKEN
```

If GHCR packages are private and the default GitHub token cannot pull from the remote host, use a PAT with package read permissions as `GHCR_READ_TOKEN`.

## Remote host expectations

Each remote host must have:

```text
Docker Engine
Docker Compose plugin
curl
rsync-compatible SSH access from GitHub Actions
```

The deployment script creates and maintains this directory structure under the configured deploy path:

```text
/opt/hometusk/
  docker-compose.yml
  .env
  .env.delivery
  .env.runtime
  postgres/
  keycloak/realm-export.json
  nginx/ssl/
```

`DEPLOY_PATH` can be changed per environment through the corresponding `*_DEPLOY_PATH` secret.

## Environment file

The `*_ENV_FILE` secret should contain the environment-specific `.env` content for Docker Compose. Example skeleton:

```dotenv
DOMAIN=uat.example.com
POSTGRES_USER=hometusk
POSTGRES_PASSWORD=change-me
POSTGRES_DB=hometusk
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=change-me
KEYCLOAK_FRONTEND_URL=https://uat.example.com
VITE_OIDC_AUTHORITY=https://uat.example.com/realms/hometusk
VITE_OIDC_CLIENT_ID=hometusk-web
VITE_OIDC_REDIRECT_URI=https://uat.example.com/callback
HOMETUSK_IDP_YANDEX_CLIENT_ID=change-me
HOMETUSK_IDP_YANDEX_CLIENT_SECRET=change-me
SPRING_PROFILES_ACTIVE=local
DECISION_PROVIDER=manual
DECISION_FALLBACK_ENABLED=true
HEALTHCHECK_PORT=80
```

Secrets must not be committed to the repository.

## Production promotion

Production deployment is a manual promotion:

1. Merge to `main`.
2. Wait for UAT deployment.
3. Verify UAT.
4. Open `Deploy Prod` workflow.
5. Enter the same immutable image tag, for example `sha-a1b2c3...`.
6. Type `prod` into `confirm_environment`.
7. Run workflow.

The production workflow does not build images. It only pulls existing images and restarts the stack.

## Rollback

Rollback is the same as deployment with the previous known-good image tag:

```text
Actions → Deploy Prod → Run workflow → image_tag=<previous sha tag> → confirm_environment=prod
```

For UAT/dev, use the manual workflow and provide a previous commit by rerunning the workflow from that commit when needed.

## Manual repository setup still required

The GitHub connector cannot safely configure all repository-level controls. Do these manually in GitHub UI:

1. Set default branch to `main`.
2. Add branch protection for `main`.
3. Add branch protection for `develop`.
4. Create environments `dev`, `uat`, `prod`.
5. Add environment secrets.
6. Add required reviewer to `prod`.
7. Delete obsolete `claude/*` branches only after their useful commits are merged or explicitly abandoned.

## Branch cleanup policy

Keep:

```text
main
develop
short-lived active feature branches
```

Delete old `claude/*` branches after confirming that their commits are merged into `main` or intentionally discarded. Do not bulk-delete unmerged branches without checking their diffs.
