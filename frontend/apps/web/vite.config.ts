import react from '@vitejs/plugin-react';
import { fileURLToPath } from 'node:url';
import { defineConfig } from 'vite';

const BACKEND_URL = 'http://localhost:8083';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
  },
  server: {
    // Gerçek backend ile çalışırken /api (giriş ve çıkış dahil) uygulamaya gider.
    proxy: {
      '/api': BACKEND_URL,
    },
  },
});
