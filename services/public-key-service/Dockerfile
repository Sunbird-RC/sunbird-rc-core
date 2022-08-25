FROM node:lts-alpine
WORKDIR /app
RUN apk add curl
ENV PATH /app/node_modules/.bin:$PATH
ENV CONFIG_BASE_PATH /etc/keys/
COPY package.json ./
COPY package-lock.json ./
RUN npm install --silent
COPY main.js ./
CMD ["npm", "start"]
