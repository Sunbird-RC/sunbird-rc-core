import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Every API path is proxied to the BFF rather than to a backend directly. That
// is not a convenience: the browser must never hold a Keycloak token, so the
// session lives in an httpOnly cookie on the BFF and all reads go through it.
// It also sidesteps CORS entirely — registry and credential-schema send no
// access-control headers, which is what made the verifier console hang on
// "Loading credential types…" before it gained a proxy.
const bff = process.env.VITE_BFF_TARGET ?? 'http://localhost:4100'

const base = process.env.VITE_BASE ?? '/issuer-portal/'
const prefix = base.replace(/\/$/, '')

// The proxy patterns carry the base prefix because the CLIENT does too (see the
// BASE comment in src/api.ts). Proxying bare `/api` instead would work in dev
// and break behind the gateway, where only the prefixed path reaches this app —
// so dev deliberately mirrors production routing rather than being laxer.
const bffPaths = ['/api', '/login', '/logout', '/callback', '/healthz'].map((p) => `${prefix}${p}`)

export default defineConfig({
  base,
  plugins: [react()],
  build: { outDir: 'dist', sourcemap: false },
  server: {
    port: 5174,
    proxy: Object.fromEntries(
      bffPaths.map((p) => [p, { target: bff, changeOrigin: true, secure: false }]),
    ),
  },
})
