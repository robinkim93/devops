FROM node:18-alpine AS build
WORKDIR /app
COPY frontend/ .
RUN npm install && npm run build

FROM nginx:1.25-alpine
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80