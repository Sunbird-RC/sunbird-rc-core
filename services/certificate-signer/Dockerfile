FROM node:lts-alpine
WORKDIR /app
ENV PATH /app/node_modules/.bin:$PATH
COPY package.json ./
COPY package-lock.json ./
RUN npm install --silent
COPY ./src ./src
COPY ./config ./config
COPY main.js ./
RUN npm test
CMD ["npm", "start"]
