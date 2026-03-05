import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/test/setup.js',
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html'],
      exclude: [
        'node_modules/',
        'src/test/',
        '**/*.config.js',
        '**/main.jsx',
        '**/vite-env.d.ts',
        // Static assets - images, fonts, etc.
        '**/*.png',
        '**/*.jpg',
        '**/*.jpeg',
        '**/*.gif',
        '**/*.svg',
        '**/*.webp',
        '**/*.ico',
        '**/*.jfif',
        '**/*.woff',
        '**/*.woff2',
        '**/*.ttf',
        '**/*.eot',
        // Style files
        '**/*.css',
      ]
    }
  },
});
