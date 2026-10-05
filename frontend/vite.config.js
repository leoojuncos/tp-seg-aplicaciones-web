import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Destinos del proxy. En el docker-compose llegan por variables de entorno; en local apuntan a
// los puertos que publica el compose.
const monolithUrl = process.env.MONOLITH_URL ?? 'http://localhost:8080';
const auditoriaUrl = process.env.AUDITORIA_URL ?? 'http://localhost:8081';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // Si el puerto esta ocupado, falla en vez de cambiarlo: el compose y el README asumen 5173.
    strictPort: true,
    // El front es el unico origen que usa el proxy. Sin CORS en el servidor de desarrollo, una
    // pagina servida en otro puerto de localhost no puede leer las respuestas de los backends.
    cors: false,
    proxy: {
      // Auditoria: /auditoria/api/... -> <auditoria>/api/...
      '/auditoria/api': {
        target: auditoriaUrl,
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/auditoria/, ''),
      },
      // Monolito, incluido el modulo messaging (/api/messaging/**). Los headers del navegador,
      // Cookie y Authorization incluidos, pasan sin cambios.
      '/api': {
        target: monolithUrl,
        changeOrigin: true,
      },
    },
  },
});
