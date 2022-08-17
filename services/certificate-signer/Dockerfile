FROM node:lts-alpine
WORKDIR /app
RUN apk add curl
ENV PATH /app/node_modules/.bin:$PATH
ENV CONFIG_BASE_PATH /etc/signer/
COPY package.json ./
COPY package-lock.json ./
RUN npm install --silent
COPY ./src ./src
COPY ./config ./config
COPY main.js ./
RUN npm test
CMD ["npm", "start"]
