import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// During development the portal runs on :5173 and forwards /api calls to the Spring Boot app on :9091.
// Same origin for the browser => no CORS configuration needed in the backend.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:9091',
    },
  },
});
