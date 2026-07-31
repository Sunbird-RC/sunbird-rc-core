# BFF: a real Node server (next start), not a static export — Route
# Handlers proxy server-side to backend services, so none of them ever
# need CORS enabled. See services/admin-ui/README or ADMIN_UI_PLAN.md.
FROM node:20.11.0-alpine3.18 AS build
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm install
COPY . .
RUN npm run build

FROM node:20.11.0-alpine3.18 AS run
WORKDIR /app
ENV NODE_ENV=production
COPY --from=build /app/.next/standalone ./
COPY --from=build /app/.next/static ./.next/static
COPY --from=build /app/public ./public
EXPOSE 3000
CMD ["node", "server.js"]
