import { fileURLToPath, URL } from 'node:url'
import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    // "@/components/ui/button" instead of "../../components/ui/button" (the shadcn/ui convention).
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
  },
  server: {
    port: 5173,
    // Forward API calls to Spring Boot during development. The browser sees a single origin (localhost:5173), so the
    // session and CSRF cookies just work, with no CORS and no hard-coded backend URL in the code.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
