FROM node:20.11.0-alpine3.18 AS install
RUN apk add curl
WORKDIR /app
COPY package.json ./
RUN npm install

FROM node:20.11.0-alpine3.18 as build
WORKDIR /app
COPY --from=install /app/node_modules ./node_modules
COPY . .
RUN npm run build

FROM node:20.11.0-alpine3.18
RUN apk add curl
WORKDIR /app
COPY --from=build /app/dist ./dist
COPY --from=build /app/package*.json ./
COPY --from=build /app/node_modules ./node_modules
EXPOSE 3400
CMD [ "npm", "run", "start:prod" ]
