FROM node:20.11.0-alpine3.18 AS install
WORKDIR /app
COPY package.json yarn.lock ./
RUN yarn

FROM node:20.11.0-alpine3.18 as build
WORKDIR /app
COPY prisma ./prisma/
COPY --from=install /app/node_modules ./node_modules
RUN apk add openssl curl
RUN npx prisma generate
COPY . .
RUN yarn build

FROM node:20.11.0-alpine3.18
WORKDIR /app
RUN apk add openssl curl
COPY --from=build /app/dist ./dist
COPY --from=build /app/package*.json ./
COPY --from=build /app/prisma ./prisma
COPY --from=build /app/node_modules ./node_modules
EXPOSE 3332
CMD [ "npm", "run", "start:migrate:prod" ]