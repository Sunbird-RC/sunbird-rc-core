// Small isomorphic helpers shared by the wallet core and clients.
// Works in Node 18+ (global fetch/TextEncoder) and in the browser (Vite).
import * as jose from 'jose';

export function b64urlFromString(str) {
  return jose.base64url.encode(new TextEncoder().encode(str));
}

// Isomorphic base64url helpers. IMPORTANT: do NOT use Buffer's 'base64url'
// encoding anywhere in this codebase — Node supports it, but the browser
// `buffer` polyfill throws "Unknown encoding: base64url", which is exactly the
// kind of Node-vs-browser drift this shared core must avoid.
export function b64urlToBytes(b64url) {
  return jose.base64url.decode(b64url); // returns Uint8Array
}

export function b64urlToString(b64url) {
  return new TextDecoder().decode(jose.base64url.decode(b64url));
}

export function bytesToB64url(bytes) {
  return jose.base64url.encode(bytes);
}

// Resolve a WebCrypto implementation lazily (no top-level await, so the
// browser build target is happy). Browser & Node 19+ have globalThis.crypto;
// Node 18 needs the webcrypto export off node:crypto.
let _cryptoPromise;
async function getCrypto() {
  if (globalThis.crypto?.getRandomValues) return globalThis.crypto;
  if (!_cryptoPromise) _cryptoPromise = import('node:crypto').then((m) => m.webcrypto);
  return _cryptoPromise;
}

export async function randomB64url(bytes = 16) {
  const arr = new Uint8Array(bytes);
  (await getCrypto()).getRandomValues(arr);
  return jose.base64url.encode(arr);
}

async function parseJsonOrThrow(res, label) {
  const text = await res.text();
  let body;
  try {
    body = text ? JSON.parse(text) : {};
  } catch {
    body = { raw: text };
  }
  if (!res.ok) {
    const msg = typeof body === 'object' ? JSON.stringify(body) : String(body);
    throw new Error(`${label} -> HTTP ${res.status}: ${msg}`);
  }
  return body;
}

export async function getJSON(url, label = 'GET') {
  const res = await fetch(url, { headers: { accept: 'application/json' } });
  return parseJsonOrThrow(res, `${label} ${url}`);
}

export async function postJSON(url, body, headers = {}, label = 'POST') {
  const res = await fetch(url, {
    method: 'POST',
    headers: { 'content-type': 'application/json', accept: 'application/json', ...headers },
    body: JSON.stringify(body ?? {}),
  });
  return parseJsonOrThrow(res, `${label} ${url}`);
}

export const PREAUTH_GRANT = 'urn:ietf:params:oauth:grant-type:pre-authorized_code';
