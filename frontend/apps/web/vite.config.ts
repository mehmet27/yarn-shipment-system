import react from '@vitejs/plugin-react';
import { fileURLToPath } from 'node:url';
import { defineConfig } from 'vite';

const GATEWAY_URL = 'http://localhost:8080';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
  },
  server: {
    // Gerçek backend ile çalışırken /api, /oauth2 ve /logout gateway'e gider.
    proxy: {
      '/api': GATEWAY_URL,
      '/oauth2': GATEWAY_URL,
      '/logout': GATEWAY_URL,
    },
  },
});
