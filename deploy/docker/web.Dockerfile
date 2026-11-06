# Frontend: statik derleme + nginx. Derleme bağlamı depo kökündür.
FROM node:22-alpine AS build
WORKDIR /src/frontend
RUN corepack enable
COPY frontend/ ./
RUN --mount=type=cache,target=/root/.local/share/pnpm/store \
    pnpm install --frozen-lockfile && pnpm --filter @iplik/web build

FROM nginx:1.27-alpine
COPY deploy/docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /src/frontend/apps/web/dist /usr/share/nginx/html
