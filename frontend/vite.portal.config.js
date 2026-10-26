import { fileURLToPath } from 'node:url';
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Portal del contribuyente: una segunda app del mismo proyecto, servida en su propio puerto como un
// sitio aparte del SGM. Su raiz es portal/ (index.html y main.jsx), que toma el resto del codigo de
// src/. En el docker-compose MONOLITH_URL llega por variable de entorno, como en vite.config.js.
const monolithUrl = process.env.MONOLITH_URL ?? 'http://localhost:8080';

export default defineConfig({
  root: fileURLToPath(new URL('./portal', import.meta.url)),
  // Cache de dependencias propia: si no, el portal y el SGM corriendo a la vez se pisan la de node_modules/.vite.
  cacheDir: fileURLToPath(new URL('./node_modules/.vite-portal', import.meta.url)),
  plugins: [react()],
  build: {
    outDir: fileURLToPath(new URL('./dist/portal', import.meta.url)),
    emptyOutDir: true,
  },
  server: {
    port: 5174,
    // Igual que el SGM: si el puerto esta ocupado falla, porque el compose y el README asumen 5174.
    strictPort: true,
    cors: false,
    proxy: {
      // Solo la API del VEP: el portal no habla con el resto del monolito ni con Auditoria.
      '/api/vep': {
        target: monolithUrl,
        changeOrigin: true,
      },
    },
  },
});
