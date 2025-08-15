import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  
  // Optional: PWA plugin configuration
  // Uncomment and install vite-plugin-pwa for advanced PWA features
  /*
  plugins: [
    react(),
    VitePWA({
      registerType: 'autoUpdate',
      workbox: {
        globPatterns: ['**/*.{js,css,html,ico,png,svg}']
      }
    })
  ],
  */
  
  build: {
    outDir: 'dist',
    sourcemap: false
  },
  
  server: {
    port: 3000,
    host: true
  }
})
