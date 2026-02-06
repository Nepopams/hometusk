/// <reference types="vitest" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  const isTest = mode === 'test' || Boolean(process.env.VITEST);

  return {
    plugins: [react()],
    test: {
      globals: true,
      environment: 'node',
    },
    server: isTest
      ? {
          host: '127.0.0.1',
          port: 0,
          hmr: false,
        }
      : {
          port: 5173,
          host: '0.0.0.0',
        },
  };
});
