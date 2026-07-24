// Age Verification demo server. Hosts two mock portals and the issuer-side
// OIDC + issuance logic:
//   /issuer    National Identity Authority portal (Keycloak login -> issue)
//   /verifier  Age Verification portal (request over_18 -> PASS/FAIL)
//
// The verifier portal talks to oid4vc-service directly from the browser (CORS is
// enabled on oid4vc-service). The issuer portal's OIDC + offer creation is
// server-side (holds the client secret + Keycloak admin creds).
import express from 'express';
import QRCode from 'qrcode';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';

const __dirname = dirname(fileURLToPath(import.meta.url));

const PORT = process.env.PORT || 4000;
const OID4VC_BASE = process.env.OID4VC_BASE || 'http://localhost:3400';
const KC_BASE = process.env.KC_BASE || 'http://localhost:8080';
const KC_REALM = process.env.KC_REALM || 'sunbird-rc';
const KC_ADMIN = process.env.KC_ADMIN || 'admin';
const KC_ADMIN_PASSWORD = process.env.KC_ADMIN_PASSWORD || 'admin123';
const CLIENT_ID = process.env.KC_CLIENT_ID || 'national-id-portal';
const CLIENT_SECRET = process.env.KC_CLIENT_SECRET || 'national-id-portal-secret';
const SELF = process.env.SELF_BASE || `http://localhost:${PORT}`;
const WALLET_URL = process.env.WALLET_URL || 'http://localhost:5555';
const REDIRECT_URI = `${SELF}/callback`;
const VCT = 'National Identity Credential';
const AUTH = `${KC_BASE}/auth`;
const OIDC = `${AUTH}/realms/${KC_REALM}/protocol/openid-connect`;

const app = express();
const states = new Map(); // state -> true

const j = (r) => r.json();
async function getJSON(url) {
  const r = await fetch(url, { headers: { accept: 'application/json' } });
  if (!r.ok) throw new Error(`GET ${url} -> ${r.status}: ${await r.text()}`);
  return j(r);
}
async function postJSON(url, body, headers = {}) {
  const r = await fetch(url, { method: 'POST', headers: { 'content-type': 'application/json', accept: 'application/json', ...headers }, body: JSON.stringify(body) });
  if (!r.ok) throw new Error(`POST ${url} -> ${r.status}: ${await r.text()}`);
  return j(r);
}

function age(dob) {
  const d = new Date(dob + 'T00:00:00Z');
  const now = new Date();
  let a = now.getUTCFullYear() - d.getUTCFullYear();
  const m = now.getUTCMonth() - d.getUTCMonth();
  if (m < 0 || (m === 0 && now.getUTCDate() < d.getUTCDate())) a--;
  return a;
}

async function kcAdminToken() {
  const r = await fetch(`${AUTH}/realms/master/protocol/openid-connect/token`, {
    method: 'POST',
    headers: { 'content-type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ grant_type: 'password', client_id: 'admin-cli', username: KC_ADMIN, password: KC_ADMIN_PASSWORD }),
  });
  if (!r.ok) throw new Error(`kc admin token -> ${r.status}`);
  return (await r.json()).access_token;
}

async function nationalIdConfigId() {
  const meta = await getJSON(`${OID4VC_BASE}/.well-known/openid-credential-issuer`);
  const supported = meta.credential_configurations_supported || meta.credentials_supported || {};
  const hit = Object.entries(supported).find(([, v]) => v.scope === VCT && v.format === 'vc+sd-jwt');
  if (!hit) throw new Error(`"${VCT}" (vc+sd-jwt) not enabled on the issuer — run: npm run seed`);
  return hit[0];
}

// --- Issuer portal OIDC ----------------------------------------------------
app.get('/issuer/login', (req, res) => {
  // Start every login from a clean slate: end any existing Keycloak SSO session
  // first, then land on /issuer/authorize which does the real auth redirect.
  // (Using prompt=login instead throws "already authenticated as different
  // user — sign out first" on legacy Keycloak; a prior logout avoids that and
  // lets you switch citizens seamlessly.)
  const u = new URL(`${OIDC}/logout`);
  u.searchParams.set('client_id', CLIENT_ID);
  u.searchParams.set('redirect_uri', `${SELF}/issuer/authorize`);
  res.redirect(u.toString());
});

// The actual OIDC authorization-code redirect (reached after the pre-logout).
app.get('/issuer/authorize', (_req, res) => {
  const state = Math.random().toString(36).slice(2);
  states.set(state, true);
  const u = new URL(`${OIDC}/auth`);
  u.searchParams.set('client_id', CLIENT_ID);
  u.searchParams.set('redirect_uri', REDIRECT_URI);
  u.searchParams.set('response_type', 'code');
  u.searchParams.set('scope', 'openid profile email');
  u.searchParams.set('state', state);
  res.redirect(u.toString());
});

// Explicit sign-out (sidebar) — ends the Keycloak session, back to /issuer.
app.get('/issuer/logout', (_req, res) => {
  const u = new URL(`${OIDC}/logout`);
  u.searchParams.set('client_id', CLIENT_ID);
  u.searchParams.set('redirect_uri', `${SELF}/issuer`);
  res.redirect(u.toString());
});

app.get('/callback', async (req, res) => {
  try {
    const { code, state } = req.query;
    if (!states.delete(state)) throw new Error('bad state');
    // Exchange code for tokens
    const tok = await (await fetch(`${OIDC}/token`, {
      method: 'POST',
      headers: { 'content-type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({ grant_type: 'authorization_code', code, redirect_uri: REDIRECT_URI, client_id: CLIENT_ID, client_secret: CLIENT_SECRET }),
    })).json();
    const userinfo = await (await fetch(`${OIDC}/userinfo`, { headers: { authorization: `Bearer ${tok.access_token}` } })).json();
    const username = userinfo.preferred_username;

    // Read the citizen's National Identity attributes from Keycloak (the
    // "National Identity System" is the source of truth).
    const admin = await kcAdminToken();
    const users = await (await fetch(`${AUTH}/admin/realms/${KC_REALM}/users?username=${encodeURIComponent(username)}&exact=true`, { headers: { authorization: `Bearer ${admin}` } })).json();
    const attrs = users[0]?.attributes || {};
    const dob = attrs.date_of_birth?.[0];
    const claims = {
      national_id: attrs.national_id?.[0] || 'NID-UNKNOWN',
      full_name: attrs.full_name?.[0] || username,
      date_of_birth: dob || '2000-01-01',
      gender: attrs.gender?.[0] || 'U',
      over_18: age(dob || '2000-01-01') >= 18,
    };

    // The National Identity Authority signs + offers the credential (OID4VCI).
    const configId = await nationalIdConfigId();
    const offer = await postJSON(`${OID4VC_BASE}/oid4vc/offer`, {
      credential_configuration_id: configId,
      format: 'vc+sd-jwt',
      claims,
    });
    const qrDataUrl = await QRCode.toDataURL(offer.qr_data, { width: 240, margin: 1 });
    res.send(renderIssued(username, claims, offer.qr_data, qrDataUrl));
  } catch (e) {
    res.status(500).send(`<pre>Issuance failed: ${e.message}</pre><p><a href="/issuer">back</a></p>`);
  }
});

// verifier config for the browser page
app.get('/verifier/config.js', (_req, res) => {
  res.type('application/javascript').send(`window.OID4VC_BASE=${JSON.stringify(OID4VC_BASE)}; window.VCT=${JSON.stringify(VCT)};`);
});

// Server-side QR so the static pages need no QR library.
app.get('/qr', async (req, res) => {
  try {
    const png = await QRCode.toBuffer(String(req.query.data || ''), { width: 240, margin: 1 });
    res.type('png').send(png);
  } catch (e) {
    res.status(400).send(e.message);
  }
});

// --- Extensible catalogs — add a card by appending an entry here ----------
const ISSUERS = [
  {
    id: 'national-id',
    icon: '🏛️',
    name: 'National Identity Authority',
    badge: 'National Identity Credential · vc+sd-jwt',
    desc: 'Authenticates you via the National Identity System (Keycloak) and issues your National Identity Credential to your wallet.',
    attrs: ['National Identity Number', 'Full Name', 'Date of Birth', 'Gender', 'Over 18'],
    loginUrl: '/issuer/login?issuer=national-id',
  },
  // Future issuers go here (each with its own Keycloak client + schema).
];

const VERIFIERS = [
  {
    id: 'age-over-18',
    icon: '🔞',
    name: 'Age Verification Portal',
    badge: 'Requests: over_18 only',
    desc: 'Prove you are over 18. Only the "Over 18" attribute is requested — date of birth and everything else stay private.',
    dcql: { credentials: [{ id: 'age', format: 'vc+sd-jwt', meta: { vct_values: [VCT] }, claims: [{ path: ['over_18'] }] }] },
    successKey: 'over_18',
  },
  // Future verification use-cases go here.
];

function issuerCard(i) {
  return `<div class="card"><div class="icon">${i.icon}</div>
    <h3>${i.name}</h3><span class="badge sd">${i.badge}</span>
    <p class="desc">${i.desc}</p>
    <ul class="attrs">${i.attrs.map((a) => `<li>${a}</li>`).join('')}</ul>
    <div class="spacer"></div>
    <a class="btn" href="${i.loginUrl}">Login &amp; Get Credential →</a></div>`;
}
function verifierCard(v) {
  return `<div class="card"><div class="icon">${v.icon}</div>
    <h3>${v.name}</h3><span class="badge">${v.badge}</span>
    <p class="desc">${v.desc}</p>
    <div class="spacer"></div>
    <button class="btn" data-verifier="${v.id}">Request proof →</button></div>`;
}

app.get(['/issuer', '/issuer/'], (_req, res) => {
  const body = `<div class="page-head"><h1>Issuer Portal</h1>
    <p>Select a credential issuer to authenticate and receive a verifiable credential in your wallet.</p></div>
    <div class="grid">${ISSUERS.map(issuerCard).join('')}</div>`;
  res.send(shell('issuer', 'Issuer Portal', body));
});

app.get(['/verifier', '/verifier/'], (_req, res) => {
  const body = `<div class="page-head"><h1>Verifier Portal</h1>
    <p>Select a verification service. It requests the minimum attributes needed — the wallet asks your consent and discloses only those.</p></div>
    <div class="grid">${VERIFIERS.map(verifierCard).join('')}</div>
    <div id="vp-panel"></div>
    <script>window.OID4VC_BASE=${JSON.stringify(OID4VC_BASE)};window.VCT=${JSON.stringify(VCT)};window.VERIFIERS=${JSON.stringify(VERIFIERS)};</script>
    <script src="/verifier/app.js"></script>`;
  res.send(shell('verifier', 'Verifier Portal', body));
});

app.use('/shared', express.static(join(__dirname, 'shared')));
app.use('/verifier', express.static(join(__dirname, 'verifier'))); // serves app.js
app.get('/', (_req, res) => res.redirect('/issuer'));

// Shared sidebar shell (also used by the server-rendered "issued" page).
function shell(active, title, body) {
  return `<!doctype html><html><head><meta charset="utf-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1"/>
  <title>${title}</title><link rel="stylesheet" href="/shared/style.css"/></head><body>
  <aside class="sidebar">
    <div class="brand"><span class="logo">🪪</span> Sunbird RC · OID4VC</div>
    <nav class="nav">
      <div class="sec">Demo</div>
      <a href="/issuer" class="${active === 'issuer' ? 'active' : ''}">🏛️ Issuer Portal</a>
      <a href="/verifier" class="${active === 'verifier' ? 'active' : ''}">🔎 Verifier Portal</a>
      <a href="${WALLET_URL}" target="_blank">👛 Open Wallet ↗</a>
      <div class="sec">Session</div>
      <a href="/issuer/logout">🚪 Sign out (switch citizen)</a>
    </nav>
    <div class="foot">Age Verification demo · vc+sd-jwt selective disclosure. Citizens: citizen.over18 / citizen.under18 (Passw0rd!).</div>
  </aside>
  <main class="main">${body}</main></body></html>`;
}

function renderIssued(username, claims, link, qrDataUrl) {
  const body = `
  <div class="page-head">
    <h1>Credential issued ✓</h1>
    <p>Authenticated as <span class="pill">${username}</span> via the National Identity System (Keycloak).
       &nbsp;<a href="/issuer/logout" style="color:var(--danger);font-weight:600">Log out / switch citizen →</a></p>
  </div>
  <div class="panel">
    <span class="badge sd">National Identity Credential · vc+sd-jwt</span>
    <div class="row">
      <div>
        <p class="muted">Issued to your wallet — every attribute is independently selectively-disclosable:</p>
        <ul class="attrs">
          <li>National Identity Number: <b>${claims.national_id}</b></li>
          <li>Full Name: <b>${claims.full_name}</b></li>
          <li>Date of Birth: <b>${claims.date_of_birth}</b></li>
          <li>Gender: <b>${claims.gender}</b></li>
          <li class="hl">Over 18: <b>${claims.over_18}</b></li>
        </ul>
        <a class="btn green" href="/verifier">Go to Age Verification Portal →</a>
      </div>
      <div class="qr">
        <p class="muted">Scan with your wallet, or paste the link below:</p>
        <img src="${qrDataUrl}" alt="offer QR"/>
        <p class="muted" style="max-width:260px"><code>${link}</code></p>
      </div>
    </div>
  </div>`;
  return shell('issuer', 'Credential Issued', body);
}

app.listen(PORT, () => {
  console.log(`Age Verification demo on ${SELF}`);
  console.log(`  Issuer Portal:   ${SELF}/issuer`);
  console.log(`  Verifier Portal: ${SELF}/verifier`);
  console.log(`  oid4vc: ${OID4VC_BASE} | keycloak: ${AUTH} (realm ${KC_REALM})`);
});
