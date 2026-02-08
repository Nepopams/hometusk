# Multi-stage build for HomeTusk Web Client
# Stage 1: Build SPA
FROM node:20-alpine AS builder

WORKDIR /app

COPY package.json package-lock.json* ./
RUN npm ci --ignore-scripts

COPY . .

# Build args become VITE_ env vars at build time
ARG VITE_API_BASE_URL=/api/v1
ARG VITE_AUTH_PROVIDER=keycloak
ARG VITE_OIDC_AUTHORITY
ARG VITE_OIDC_CLIENT_ID=hometusk-web
ARG VITE_OIDC_REDIRECT_URI

RUN npm run build

# Stage 2: Serve with nginx
FROM nginx:1.25-alpine AS runtime

# Remove default config
RUN rm /etc/nginx/conf.d/default.conf

# Copy built SPA
COPY --from=builder /app/dist /usr/share/nginx/html

# SPA fallback: all routes -> index.html
RUN printf 'server {\n\
    listen 80;\n\
    root /usr/share/nginx/html;\n\
    index index.html;\n\
    location / {\n\
        try_files $uri $uri/ /index.html;\n\
    }\n\
}\n' > /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=15s --timeout=3s --retries=3 \
    CMD wget -q --spider http://localhost:80/ || exit 1
