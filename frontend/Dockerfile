# Stage 1: Build
FROM node:20-alpine AS build

WORKDIR /app

ARG VITE_API_URL=/api
ARG VITE_WS_URL=/ws
ENV VITE_API_URL=${VITE_API_URL}
ENV VITE_WS_URL=${VITE_WS_URL}

# Copia package.json e package-lock.json primeiro para cache
COPY package.json package-lock.json* ./

# Instala dependências
RUN npm ci

# Copia código fonte e build
COPY . .
RUN npm run build

# Stage 2: Runtime com Nginx
FROM nginx:alpine

# Remove config padrão e adiciona a nossa
RUN rm /etc/nginx/conf.d/default.conf

# Copia a configuração do Nginx
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copia arquivos estáticos do build
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 4200

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://127.0.0.1:4200/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
