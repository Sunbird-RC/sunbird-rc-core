import { defineConfig } from 'vite';
import { nodePolyfills } from 'vite-plugin-node-polyfills';

// The wallet core (src/*.mjs) is shared verbatim with the headless Node runner
// and depends on Buffer + WebCrypto; @auth0/mdl (mdoc) needs Node globals in
// the browser too. The polyfill plugin supplies Buffer/process/crypto so the
// exact same code path runs in the browser as in `npm run e2e`.
export default defineConfig({
  // Relative asset base so the built app works whether served at the web root
  // (local `vite preview`) OR under a gateway subpath (deployed behind nginx at
  // `/wallet/` on the VM) — no rebuild needed per host.
  base: './',
  plugins: [
    nodePolyfills({
      globals: { Buffer: true, global: true, process: true },
      protocolImports: true,
    }),
  ],
  server: { port: 5555, host: true },
});
