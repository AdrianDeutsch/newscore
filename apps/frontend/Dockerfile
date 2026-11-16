# syntax=docker/dockerfile:1

# ---- Build stage ---------------------------------------------------------
FROM node:24-alpine AS build
WORKDIR /app

# Install exactly what the lockfile pins; a lockfile that is out of sync fails the build.
COPY package.json package-lock.json ./
RUN npm ci

# Build the SSR (nitro node-server) bundle
COPY . .
RUN npm run build

# ---- Runtime stage -------------------------------------------------------
FROM node:24-alpine AS runtime
WORKDIR /app

ENV NODE_ENV=production
ENV HOST=0.0.0.0
ENV PORT=3000

COPY --from=build /app/.output ./.output

USER node
EXPOSE 3000
CMD ["node", ".output/server/index.mjs"]
