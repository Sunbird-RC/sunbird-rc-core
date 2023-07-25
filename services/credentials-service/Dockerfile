FROM node:16 AS install
RUN apt-get update \
    && apt-get install -y wkhtmltopdf
WORKDIR /app
COPY package.json yarn.lock ./
RUN yarn

FROM node:16 as build
WORKDIR /app
COPY prisma ./prisma/
COPY --from=install /app/node_modules ./node_modules
RUN npx prisma generate
COPY . .
RUN yarn build

FROM node:16
RUN apt-get update \
    && apt-get install -y wkhtmltopdf=0.12.5-1

WORKDIR /app
COPY --from=build /app/dist ./dist
COPY --from=build /app/package*.json ./
COPY --from=build /app/prisma ./prisma
COPY --from=build /app/node_modules ./node_modules
EXPOSE 3000
CMD [ "npm", "run", "start:migrate:prod" ]