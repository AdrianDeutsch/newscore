# syntax=docker/dockerfile:1

# ---- Build stage ---------------------------------------------------------
FROM node:26-alpine AS build
WORKDIR /app

# Install dependencies (use the lockfile when present for reproducible builds)
COPY package.json package-lock.json* ./
RUN npm ci || npm install

# Build the SSR (nitro node-server) bundle
COPY . .
RUN npm run build

# ---- Runtime stage -------------------------------------------------------
FROM node:26-alpine AS runtime
WORKDIR /app

ENV NODE_ENV=production
ENV HOST=0.0.0.0
ENV PORT=3000

COPY --from=build /app/.output ./.output

USER node
EXPOSE 3000
CMD ["node", ".output/server/index.mjs"]
