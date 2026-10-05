FROM node:22-alpine
WORKDIR /app
RUN chown node:node /app
USER node
COPY --chown=node:node package.json package-lock.json ./
RUN --mount=type=cache,target=/home/node/.npm,uid=1000,gid=1000 npm ci
COPY --chown=node:node . .
EXPOSE 5173
# Servidor de desarrollo de Vite: su proxy es lo que une al front con los dos backends.
# --host 0.0.0.0 va solo aca; desde el host se entra por el puerto que publica el compose.
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0"]
