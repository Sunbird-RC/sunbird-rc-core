# Static build served by nginx. Nothing at runtime talks to the API from the
# server side — the browser calls oid4vc-service directly, same-origin behind the
# main nginx, so there is no backend of its own to configure.
FROM node:20.11.0-alpine3.18 AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# Must match the nginx location this is mounted at.
ARG VITE_BASE=/verifier-app/
ENV VITE_BASE=$VITE_BASE
RUN npm run build

FROM nginx:1.27-alpine
RUN apk add --no-cache curl
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
