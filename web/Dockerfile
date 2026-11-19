FROM node:24-alpine AS build
WORKDIR /proyecto
ENV NEXT_TELEMETRY_DISABLED=1
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npm run build && node scripts/huellas-csp.mjs

FROM nginxinc/nginx-unprivileged:1.30-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /proyecto/scripts/csp.conf /etc/nginx/csp.conf
COPY --from=build /proyecto/out /usr/share/nginx/html
