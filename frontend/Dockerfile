FROM node:22-alpine AS build
WORKDIR /workspace
ARG VITE_API_BASE=/api
ARG VITE_BASE_PATH=/
ENV VITE_API_BASE=${VITE_API_BASE}
ENV VITE_BASE_PATH=${VITE_BASE_PATH}
COPY frontend/package.json ./package.json
RUN npm install
COPY frontend/ ./
RUN npm run build

FROM nginx:1.27-alpine
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist /usr/share/nginx/html
EXPOSE 80
