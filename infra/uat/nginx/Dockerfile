# Multi-stage: build SPA + serve via nginx with reverse proxy
#
# Stage 1: Build web client
FROM node:20-alpine AS spa-builder

WORKDIR /app

COPY clients/web/package.json clients/web/package-lock.json* ./
RUN npm ci --ignore-scripts

COPY clients/web/ .

ARG VITE_API_BASE_URL=/api/v1
ARG VITE_AUTH_PROVIDER=keycloak
ARG VITE_OIDC_AUTHORITY
ARG VITE_OIDC_CLIENT_ID=hometusk-web
ARG VITE_OIDC_REDIRECT_URI

RUN npm run build

# Stage 2: Nginx with SPA + reverse proxy config
FROM nginx:1.25-alpine

RUN rm /etc/nginx/conf.d/default.conf

COPY infra/uat/nginx/nginx.conf /etc/nginx/nginx.conf
COPY --from=spa-builder /app/dist /usr/share/nginx/html

EXPOSE 80 443

HEALTHCHECK --interval=15s --timeout=3s --retries=3 \
    CMD wget -q --spider http://127.0.0.1:80/healthz || exit 1
