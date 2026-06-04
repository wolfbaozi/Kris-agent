FROM docker.m.daocloud.io/node:20-alpine

WORKDIR /app

COPY package.json ./
RUN npm install -g npm@latest && npm config set registry https://registry.npmmirror.com && npm install --omit=dev

COPY . .

EXPOSE 3001

CMD ["node", "index.js"]
