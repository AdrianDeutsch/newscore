# syntax=docker/dockerfile:1

# ---- Build stage ---------------------------------------------------------
FROM node:24-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# ---- Runtime stage -------------------------------------------------------
FROM node:24-alpine AS runtime
WORKDIR /app
ENV NODE_ENV=production
# kafkajs 2.2.4 passes a negative delay to setTimeout once per connection; Node 24 reports that as
# TimeoutNegativeWarning. Node clamps the value to 1 ms, which is what kafkajs intends, so only this
# one warning type is switched off. (kafkajs has had no release since 2023; see the README.)
ENV NODE_OPTIONS=--disable-warning=TimeoutNegativeWarning

COPY package.json package-lock.json ./
RUN npm ci --omit=dev
COPY --from=build /app/dist ./dist

# Numeric (the image's "node" user): with runAsNonRoot, Kubernetes can only verify a numeric UID.
USER 1000
CMD ["node", "dist/index.js"]
