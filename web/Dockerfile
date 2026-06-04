FROM docker.m.daocloud.io/node:20-alpine AS builder

WORKDIR /app

COPY package.json ./
RUN npm install -g npm@latest && npm config set registry https://registry.npmmirror.com && npm install

COPY . .
RUN npm run build

FROM docker.m.daocloud.io/nginx:stable-alpine

COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80
