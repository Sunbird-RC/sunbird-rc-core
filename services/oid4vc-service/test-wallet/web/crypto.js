// Client-side envelope encryption for the holder key. The private key is
// encrypted here (PBKDF2 → AES-GCM, keyed by the user's password) BEFORE it is
// sent to the wallet backend — the server only ever stores ciphertext. Uses
// WebCrypto, which requires a secure context (HTTPS or localhost); the account
// feature therefore needs the wallet served over https:// or on localhost.
const te = new TextEncoder();
const td = new TextDecoder();

const b64 = (buf) => btoa(String.fromCharCode(...new Uint8Array(buf)));
const unb64 = (s) => Uint8Array.from(atob(s), (c) => c.charCodeAt(0));

async function deriveKey(password, salt) {
  const base = await crypto.subtle.importKey('raw', te.encode(password), 'PBKDF2', false, ['deriveKey']);
  return crypto.subtle.deriveKey(
    { name: 'PBKDF2', salt, iterations: 150000, hash: 'SHA-256' },
    base,
    { name: 'AES-GCM', length: 256 },
    false,
    ['encrypt', 'decrypt'],
  );
}

// Returns a compact "salt.iv.ciphertext" (all base64) string.
export async function encryptJson(obj, password) {
  const salt = crypto.getRandomValues(new Uint8Array(16));
  const iv = crypto.getRandomValues(new Uint8Array(12));
  const key = await deriveKey(password, salt);
  const ct = await crypto.subtle.encrypt({ name: 'AES-GCM', iv }, key, te.encode(JSON.stringify(obj)));
  return `${b64(salt)}.${b64(iv)}.${b64(ct)}`;
}

export async function decryptJson(str, password) {
  const [s, i, c] = str.split('.');
  const key = await deriveKey(password, unb64(s));
  const pt = await crypto.subtle.decrypt({ name: 'AES-GCM', iv: unb64(i) }, key, unb64(c));
  return JSON.parse(td.decode(pt));
}
