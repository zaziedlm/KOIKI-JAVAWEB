import { defineConfig } from 'vite';
export default defineConfig({
  base: '/spa/',
  build: { outDir: '../target/b1-frontend', emptyOutDir: true, target: 'es2022',
    sourcemap: false, manifest: true, assetsDir: 'assets' }
});
