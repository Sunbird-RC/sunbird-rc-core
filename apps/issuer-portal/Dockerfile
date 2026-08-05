# Unlike verifier-app this is NOT a static nginx image: the BFF has to run,
# because the Keycloak client is confidential and its secret must stay
# server-side. Build the bundle, then ship it alongside the BFF on node:alpine.
FROM node:22-alpine AS build
WORKDIR /app
# Copy manifests first so `npm ci` is cached independently of source changes.
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY tsconfig.json vite.config.ts index.html ./
COPY src ./src
# Static assets vite copies into dist verbatim — the issuer logos live here.
# Without this the logos 404 in the image and every issuer silently falls back to
# its emoji icon, which looks like a data problem rather than a missing COPY.
COPY public ./public
RUN npm run build

FROM node:22-alpine
WORKDIR /app
# curl for the compose healthcheck; the BFF itself has zero runtime dependencies,
# so no node_modules are copied into the final image at all.
RUN apk add --no-cache curl
COPY --from=build /app/dist ./dist
COPY bff ./bff
ENV NODE_ENV=production PORT=4100
EXPOSE 4100
# Run unprivileged — the image already provides the `node` user.
USER node
CMD ["node", "bff/server.mjs"]
