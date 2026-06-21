# syntax=docker/dockerfile:1

# ---- Build stage ---------------------------------------------------------
FROM node:26-alpine AS build
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm ci || npm install
COPY . .
RUN npm run build

# ---- Runtime stage -------------------------------------------------------
FROM node:26-alpine AS runtime
WORKDIR /app
ENV NODE_ENV=production

COPY package.json package-lock.json* ./
RUN npm ci --omit=dev || npm install --omit=dev
COPY --from=build /app/dist ./dist

USER node
CMD ["node", "dist/index.js"]
