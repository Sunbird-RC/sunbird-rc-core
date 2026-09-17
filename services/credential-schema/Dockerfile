FROM node:20.11.0-alpine3.18 AS install
RUN apk add openssl curl
WORKDIR /app
COPY package.json yarn.lock ./
RUN yarn

FROM node:20.11.0-alpine3.18 as build
RUN apk add openssl curl
WORKDIR /app
COPY prisma ./prisma/
COPY --from=install /app/node_modules ./node_modules
RUN npx prisma generate
COPY . .
RUN yarn build

FROM node:20.11.0-alpine3.18
RUN apk add openssl curl
WORKDIR /app
COPY --from=build /app/dist ./dist
COPY --from=build /app/package*.json ./
COPY --from=build /app/prisma ./prisma
COPY --from=build /app/node_modules ./node_modules
EXPOSE 3000

# [Auto-Remediation CWE-250] Enforce non-root execution
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
USER appuser
CMD [ "npm", "run", "start:migrate:prod" ]