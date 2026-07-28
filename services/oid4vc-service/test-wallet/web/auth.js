// Wallet-backend client: accounts (signup/login), the encrypted holder blob,
// and per-user credential persistence. Behind nginx the backend is same-origin
// at /wallet-api; on the Vite dev server (:5555) it's on :4100.
const WB = () =>
  (location.port === '5555' ? `${location.protocol}//${location.hostname}:4100` : location.origin) + '/wallet-api';

let token = sessionStorage.getItem('wallet_token') || null;
let username = sessionStorage.getItem('wallet_user') || null;

export const currentUser = () => username;
export const isLoggedIn = () => !!token;

async function api(method, path, body) {
  const res = await fetch(WB() + path, {
    method,
    headers: { 'content-type': 'application/json', ...(token ? { authorization: `Bearer ${token}` } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  });
  if (!res.ok) {
    let msg = `${res.status}`;
    try { msg = (await res.json()).error || msg; } catch { /* ignore */ }
    throw new Error(msg);
  }
  return res.json();
}

function setSession(t, u) {
  token = t; username = u;
  sessionStorage.setItem('wallet_token', t);
  sessionStorage.setItem('wallet_user', u);
}
export function logout() {
  token = null; username = null;
  sessionStorage.removeItem('wallet_token');
  sessionStorage.removeItem('wallet_user');
  sessionStorage.removeItem('wallet_holder'); // cached privateJwk (see app.js)
}

export async function signup(u, p) { setSession(...pick(await api('POST', '/signup', { username: u, password: p }))); }
export async function login(u, p) { setSession(...pick(await api('POST', '/login', { username: u, password: p }))); }
const pick = (r) => [r.token, r.username];

// No auth required — this is the public verifier allowlist, not user data.
export const getTrustedVerifiers = () => api('GET', '/trusted-verifiers');

export const getHolder = () => api('GET', '/holder');
export const putHolder = (enc_holder, did) => api('PUT', '/holder', { enc_holder, did });
export const resetHolder = () => api('DELETE', '/holder');
export const getCredentials = () => api('GET', '/credentials');
export const addCredentialApi = (rec) => api('POST', '/credentials', rec);
export const deleteCredentialApi = (id) => api('DELETE', `/credentials/${id}`);
