import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// During development the React app runs on http://localhost:5173 and forwards
// every /api request to the Spring Boot server on port 8080.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
});
