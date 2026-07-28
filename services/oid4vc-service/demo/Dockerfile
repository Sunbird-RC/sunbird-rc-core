FROM node:20-alpine
WORKDIR /app
COPY package.json ./
RUN npm install --omit=dev --no-audit --no-fund
COPY server.mjs seed-national-id.mjs ./
COPY keycloak ./keycloak
COPY shared ./shared
COPY verifier ./verifier
EXPOSE 4000
CMD ["node", "server.mjs"]
