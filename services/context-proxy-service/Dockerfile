FROM node:lts-alpine
WORKDIR /app
RUN apk add curl
ENV PATH /app/node_modules/.bin:$PATH
COPY package.json ./
COPY package-lock.json ./
RUN npm install --silent
COPY main.js ./
CMD ["npm", "start"]
