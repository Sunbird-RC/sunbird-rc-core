import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Served behind nginx at /verifier-app/ on the same host as oid4vc-service, so
// asset URLs must be relative to that prefix. `base` is overridable at build
// time (VITE_BASE) for anyone mounting it elsewhere.
//
// The dev server proxies the API paths to a real deployment so local development
// is same-origin, exactly like production. That is not merely convenient: only
// oid4vc-service sends `access-control-allow-origin`, while `/credential-schema`
// (a different service behind the same gateway) sends none — so a browser at
// localhost calling it with `?base=https://host` is blocked by CORS and the
// credential-type list never loads.
const target = process.env.VITE_API_TARGET ?? 'https://98.70.36.106.sslip.io'

const apiPaths = ['/credential-schema', '/.well-known', '/vp', '/oid4vc', '/vct', '/qr', '/did']

export default defineConfig({
  base: process.env.VITE_BASE ?? '/verifier-app/',
  plugins: [react()],
  build: { outDir: 'dist', sourcemap: false },
  server: {
    proxy: Object.fromEntries(
      apiPaths.map((p) => [p, { target, changeOrigin: true, secure: true }]),
    ),
  },
})
