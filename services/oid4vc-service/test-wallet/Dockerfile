# Multi-stage: build the Vite static bundle, serve it with nginx.
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install --no-audit --no-fund
COPY . .
RUN npm run build

FROM nginx:alpine
# SPA assets use a relative base (vite base './'), so they resolve correctly
# whether this is fronted at '/' or under the gateway's '/wallet/' prefix.
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
