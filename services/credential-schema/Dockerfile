FROM node:16 as dependencies
WORKDIR /app
COPY . ./
RUN yarn
EXPOSE 3000
CMD ["yarn", "start"]