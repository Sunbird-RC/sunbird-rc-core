FROM node:20.11.0-slim AS install
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends git openssh-client ca-certificates \
  && rm -rf /var/lib/apt/lists/* \
  && git config --global url."https://github.com/".insteadOf "ssh://git@github.com/" \
  && git config --global url."https://github.com/".insteadOf "git@github.com:"

COPY package.json yarn.lock ./
RUN sed -i 's|git+ssh://git@github.com/|git+https://github.com/|g' yarn.lock \
  && yarn

FROM node:20.11.0-slim as build
WORKDIR /app

COPY prisma ./prisma/
COPY --from=install /app/node_modules ./node_modules

RUN apt-get update && apt-get install -y --no-install-recommends openssl curl ca-certificates \
  && rm -rf /var/lib/apt/lists/*

RUN npx prisma generate
COPY . .
RUN yarn build

FROM node:20.11.0-slim
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends openssl curl ca-certificates \
  && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/dist ./dist
COPY --from=build /app/package*.json ./
COPY --from=build /app/prisma ./prisma
COPY --from=build /app/node_modules ./node_modules
EXPOSE 3332
CMD [ "npm", "run", "start:migrate:prod" ]