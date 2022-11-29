FROM node:lts-alpine3.13 as verification_ui
WORKDIR /app
ENV PATH /app/node_modules/.bin:$PATH
COPY services/verification-ui ./
RUN npm install
RUN npm run build

FROM nginx:stable-alpine
COPY --from=verification_ui /app/build /usr/share/nginx/html
COPY nginx/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]