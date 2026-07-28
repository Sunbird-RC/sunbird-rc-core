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
// Behind a reverse proxy the browser-facing URLs differ from the in-network
// ones: redirects (auth/logout) must use the public bases, while token /
// userinfo / admin calls stay on the internal ones.
const KC_PUBLIC = process.env.KC_PUBLIC_BASE || KC_BASE;
const OIDC_PUB = `${KC_PUBLIC}/auth/realms/${KC_REALM}/protocol/openid-connect`;
const OID4VC_PUBLIC = process.env.OID4VC_PUBLIC || OID4VC_BASE;

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

async function metaSupported() {
  const meta = await getJSON(`${OID4VC_BASE}/.well-known/openid-credential-issuer`);
  return meta.credential_configurations_supported || meta.credentials_supported || {};
}
// Resolve the credential_configuration_id for a given schema name + format.
async function configIdFor(schemaName, format) {
  const supported = await metaSupported();
  const hit = Object.entries(supported).find(([, v]) => v.scope === schemaName && v.format === format);
  return hit && hit[0];
}

// Maps each of a verifier's `typeNames` (human schema names, e.g. "National
// Identity Credential") to the actual `vct` the issuer publishes for its
// vc+sd-jwt config. Needed because DCQL's `vct_values` must match the `vct`
// claim embedded in the presented credential — which oid4vc-service now
// normalizes to an absolute URI (see vct.util.ts normalizeVct()) for any
// schema whose vct isn't already one. Found live: the verifier was still
// asking for the bare display name, so walt.id — holding a credential whose
// real vct is the URI — reported "no credentials matching this presentation
// request" even though the credential was issued and stored correctly.
// Falls back to the bare name if no vc+sd-jwt config is found for that scope.
async function resolveSdJwtVctValues(typeNames) {
  const supported = await metaSupported();
  const byScope = new Map();
  for (const cfg of Object.values(supported)) {
    if (cfg.format === 'vc+sd-jwt' && cfg.scope) byScope.set(cfg.scope, cfg.vct || cfg.scope);
  }
  return typeNames.map((name) => byScope.get(name) || name);
}

// --- Issuer portal OIDC ----------------------------------------------------
app.get('/issuer/login', (req, res) => {
  // Start every login from a clean slate: end any existing Keycloak SSO session
  // first, then land on /issuer/authorize which does the real auth redirect.
  // Carry the chosen issuer + credential format through the logout hop.
  const issuer = req.query.issuer || 'national-id';
  const format = req.query.format || 'vc+sd-jwt';
  const u = new URL(`${OIDC_PUB}/logout`);
  u.searchParams.set('client_id', CLIENT_ID);
  u.searchParams.set('redirect_uri', `${SELF}/issuer/authorize?issuer=${encodeURIComponent(issuer)}&format=${encodeURIComponent(format)}`);
  res.redirect(u.toString());
});

// The actual OIDC authorization-code redirect (reached after the pre-logout).
app.get('/issuer/authorize', (req, res) => {
  const state = Math.random().toString(36).slice(2);
  states.set(state, { issuerId: req.query.issuer || 'national-id', format: req.query.format || 'vc+sd-jwt' });
  const u = new URL(`${OIDC_PUB}/auth`);
  u.searchParams.set('client_id', CLIENT_ID);
  u.searchParams.set('redirect_uri', REDIRECT_URI);
  u.searchParams.set('response_type', 'code');
  u.searchParams.set('scope', 'openid profile email');
  u.searchParams.set('state', state);
  res.redirect(u.toString());
});

// Explicit sign-out (sidebar) — ends the Keycloak session, back to /issuer.
app.get('/issuer/logout', (_req, res) => {
  const u = new URL(`${OIDC_PUB}/logout`);
  u.searchParams.set('client_id', CLIENT_ID);
  u.searchParams.set('redirect_uri', `${SELF}/issuer`);
  res.redirect(u.toString());
});

app.get('/callback', async (req, res) => {
  try {
    const { code, state } = req.query;
    const st = states.get(state);
    states.delete(state);
    if (!st) throw new Error('bad state');
    const issuer = ISSUERS.find((i) => i.id === st.issuerId) || ISSUERS[0];
    const format = st.format || 'vc+sd-jwt';

    // Exchange code for tokens + read the authenticated user's attributes.
    const tok = await (await fetch(`${OIDC}/token`, {
      method: 'POST',
      headers: { 'content-type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({ grant_type: 'authorization_code', code, redirect_uri: REDIRECT_URI, client_id: CLIENT_ID, client_secret: CLIENT_SECRET }),
    })).json();
    const userinfo = await (await fetch(`${OIDC}/userinfo`, { headers: { authorization: `Bearer ${tok.access_token}` } })).json();
    const username = userinfo.preferred_username;
    const admin = await kcAdminToken();
    const users = await (await fetch(`${AUTH}/admin/realms/${KC_REALM}/users?username=${encodeURIComponent(username)}&exact=true`, { headers: { authorization: `Bearer ${admin}` } })).json();
    const attrs = users[0]?.attributes || {};

    // Build the credential subject for THIS issuer from the user's attributes.
    const claims = issuer.claims(attrs, username);

    // Resolve the config id for the chosen schema + format, then offer it.
    const configId = await configIdFor(issuer.schemaName, format);
    if (!configId) throw new Error(`No config for "${issuer.schemaName}" (${format}) — run seed-demo.mjs`);
    const offer = await postJSON(`${OID4VC_BASE}/oid4vc/offer`, { credential_configuration_id: configId, format, claims });
    const qrDataUrl = await QRCode.toDataURL(offer.qr_data, { width: 240, margin: 1 });
    res.send(await renderIssuerPage(issuer.id, { issuer, username, claims, format, link: offer.qr_data, qrDataUrl }));
  } catch (e) {
    res.status(500).send(`<pre>Issuance failed: ${e.message}</pre><p><a href="/issuer">back</a></p>`);
  }
});

// verifier config for the browser page
app.get('/verifier/config.js', (_req, res) => {
  res.type('application/javascript').send(`window.OID4VC_BASE=${JSON.stringify(OID4VC_PUBLIC)}; window.VCT=${JSON.stringify(VCT)};`);
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
const ALL4 = ['ldp_vc', 'jwt_vc_json', 'vc+sd-jwt', 'mso_mdoc'];
const av = (a, k, d) => (a[k]?.[0]) ?? d; // first value of a Keycloak attribute

function academicClaims(a, u) {
  return {
    learner_id: av(a, 'learner_id', 'EDU-0000'),
    full_name: av(a, 'full_name', u),
    institution_name: av(a, 'institution_name', 'Institution'),
    qualification: av(a, 'qualification', '-'),
    programme: av(a, 'programme', '-'),
    completion_date: av(a, 'completion_date', '2020-01-01'),
    academic_result: av(a, 'academic_result', '-'),
  };
}

const ISSUERS = [
  { id: 'national-id', tab: 'National Identity', icon: '🏛️', group: 'National Identity', name: 'National Identity Authority',
    schemaName: 'National Identity Credential',
    desc: 'Authenticates you via the National Identity System (Keycloak) and issues your National Identity Credential.',
    attrs: ['National Identity Number', 'Full Name', 'Date of Birth', 'Gender', 'Over 18'],
    claims: (a, u) => { const dob = av(a, 'date_of_birth', '2000-01-01'); return {
      national_id: av(a, 'national_id', 'NID-UNKNOWN'), full_name: av(a, 'full_name', u),
      date_of_birth: dob, gender: av(a, 'gender', 'U'), over_18: age(dob) >= 18 }; } },

  { id: 'farmer', tab: 'Farmer Registry', icon: '🌾', group: 'Agriculture · Farmer Registry', name: 'Farmer Registry Authority',
    schemaName: 'Farmer Credential',
    desc: 'Validates your identity + agricultural records and issues a Farmer Credential for loan processing.',
    attrs: ['Farmer ID', 'Full Name', 'Gender', 'Land Area (acres)', 'Ownership Type', 'Land Record Ref', 'Primary Crop', 'Farm Location'],
    claims: (a, u) => ({
      farmer_id: av(a, 'farmer_id', 'FRM-0000'), full_name: av(a, 'full_name', u), gender: av(a, 'gender', 'U'),
      land_area_acres: Number(av(a, 'land_area_acres', '0')), ownership_type: av(a, 'ownership_type', 'Owned'),
      land_record_ref: av(a, 'land_record_ref', '-'), primary_crop: av(a, 'primary_crop', '-'), farm_location: av(a, 'farm_location', '-') }) },

  { id: 'edu-secondary', tab: 'Secondary Board', icon: '🏫', group: 'Education · Institutions', name: 'State Secondary Education Board',
    schemaName: 'Secondary School Certificate',
    desc: 'Issues a Secondary School Certificate as an Academic Credential.',
    attrs: ['Learner ID', 'Full Name', 'Institution', 'Qualification', 'Programme', 'Completion Date', 'Result'],
    claims: academicClaims },
  { id: 'edu-university', tab: 'University', icon: '🎓', group: 'Education · Institutions', name: 'State University',
    schemaName: 'University Degree',
    desc: 'Issues a University Degree as an Academic Credential.',
    attrs: ['Learner ID', 'Full Name', 'Institution', 'Qualification', 'Programme', 'Completion Date', 'Result'],
    claims: academicClaims },
  { id: 'edu-pg', tab: 'PG Institute', icon: '🎓', group: 'Education · Institutions', name: 'National Postgraduate Institute',
    schemaName: 'Postgraduate Degree',
    desc: 'Issues a Postgraduate Degree as an Academic Credential.',
    attrs: ['Learner ID', 'Full Name', 'Institution', 'Qualification', 'Programme', 'Completion Date', 'Result'],
    claims: academicClaims },
];

// Verifier catalog. request = claim names; the client builds format-specific
// DCQL from vct / docType(+namespace) / typeNames.
const VERIFIERS = [
  { id: 'age', tab: 'Age Verification', icon: '🔞', group: 'Identity', name: 'Age Verification Portal',
    desc: 'Prove you are over 18. Only the "Over 18" attribute is requested — DOB and everything else stay private.',
    vct: 'National Identity Credential', docType: null, namespace: null,
    typeNames: ['National Identity Credential'], request: ['over_18'], successKey: 'over_18', formats: ['vc+sd-jwt'] },

  { id: 'rural-credit', tab: 'Rural Credit', icon: '🏦', group: 'Agriculture', name: 'Rural Credit Portal',
    desc: 'A bank verifying a farmer for an agricultural loan. Requests only land ownership + farm details — identity stays private.',
    vct: 'Farmer Credential', docType: 'in.gov.farmer.1', namespace: 'in.gov.farmer.1',
    typeNames: ['Farmer Credential'], request: ['land_area_acres', 'ownership_type', 'land_record_ref', 'primary_crop'], formats: ALL4 },

  { id: 'education', tab: 'Education', icon: '🎓', group: 'Education', name: 'Education Verification Portal',
    desc: 'An employer/university verifying a qualification. Requests only qualification details — not full learner identity.',
    vct: 'Academic Credential', docType: 'edu.academic.1', namespace: 'edu.academic.1',
    typeNames: ['Secondary School Certificate', 'University Degree', 'Postgraduate Degree'],
    request: ['qualification', 'programme', 'academic_result', 'institution_name'], formats: ALL4 },
];

const FMT_LABEL = { ldp_vc: 'ldp_vc (JSON-LD)', jwt_vc_json: 'jwt_vc_json', 'vc+sd-jwt': 'vc+sd-jwt ✓ SD', mso_mdoc: 'mso_mdoc ✓ SD' };

// Right-hand column of an issuer tab: QR/status shown BESIDE the issuer info.
function issuerRight(i, issued) {
  if (issued && issued.issuer.id === i.id) {
    const rows = Object.entries(issued.claims).map(([k, v]) => `<li>${k}: <b>${v}</b></li>`).join('');
    const sd = issued.format === 'vc+sd-jwt' || issued.format === 'mso_mdoc';
    return `<div class="qr-panel issued">
      <div class="ok-badge">✓ Issued as ${issued.username}</div>
      <img src="${issued.qrDataUrl}" alt="offer QR"/>
      <p class="muted">Scan with your wallet, or copy the offer link:</p>
      <textarea class="offer-link" readonly onclick="this.select()">${issued.link}</textarea>
      <p class="muted">${sd ? 'Selectively disclosable at presentation.' : 'Whole credential shared at presentation.'}</p>
      <a class="btn green" href="/verifier">Go to Verifier →</a></div>`;
  }
  return `<div class="qr-panel empty"><div class="qr-ph">🔐</div>
    <p class="muted">Log in above to generate your credential offer — the QR appears here.</p></div>`;
}

// One issuer tab panel: issuer details (left) + QR/status (right, beside).
function issuerPanel(i, formats, activeId, issued) {
  const opts = (formats.length ? formats : ['vc+sd-jwt']).map((f) => `<option value="${f}"${issued && issued.issuer.id === i.id && issued.format === f ? ' selected' : ''}>${FMT_LABEL[f] || f}</option>`).join('');
  return `<div class="tab-panel ${i.id === activeId ? 'active' : ''}" data-panel="${i.id}">
    <div class="issuer-split">
      <div class="issuer-info">
        <div class="icon">${i.icon}</div><h3>${i.name}</h3>
        <span class="badge sd">${i.schemaName}</span>
        <p class="desc">${i.desc}</p>
        <ul class="attrs">${i.attrs.map((a) => `<li>${a}</li>`).join('')}</ul>
        <label class="fmt-label">Format <select class="fmt">${opts}</select></label>
        <button class="btn" data-issuer="${i.id}">Login &amp; Get Credential →</button>
      </div>
      ${issuerRight(i, issued)}
    </div></div>`;
}
// One verifier tab panel: verifier details (left) + QR/result (right, beside).
// The right ".vp-out" is filled client-side by verifier/app.js.
function verifierPanel(v, activeId) {
  const opts = v.formats.map((f) => `<option value="${f}">${FMT_LABEL[f] || f}</option>`).join('');
  return `<div class="tab-panel ${v.id === activeId ? 'active' : ''}" data-panel="${v.id}">
    <div class="issuer-split">
      <div class="issuer-info">
        <div class="icon">${v.icon}</div><h3>${v.name}</h3>
        <span class="badge">Requests: ${v.request.join(', ')}</span>
        <p class="desc">${v.desc}</p>
        <label class="fmt-label">Format <select class="fmt">${opts}</select></label>
        <button class="btn" data-verifier="${v.id}">Request proof →</button>
      </div>
      <div class="qr-panel empty vp-out"><div class="qr-ph">📷</div>
        <p class="muted">Click "Request proof" to generate a QR — scan it with your wallet; the result appears here.</p></div>
    </div></div>`;
}

async function renderIssuerPage(activeId, issued) {
  const supported = await metaSupported();
  const fmtsFor = (name) => ALL4.filter((f) => Object.values(supported).some((v) => v.scope === name && v.format === f));
  const tabs = ISSUERS.map((i) => `<button data-tab="${i.id}" class="${i.id === activeId ? 'active' : ''}">${i.tab || i.name}</button>`).join('');
  const panels = ISSUERS.map((i) => issuerPanel(i, fmtsFor(i.schemaName), activeId, issued)).join('');
  const body = `<div class="page-head"><h1>Issuer Portal</h1>
    <p>Pick an issuer tab, choose a format, then authenticate. Your credential offer (QR) appears beside the issuer.</p></div>
    <div class="tabs">${tabs}</div>
    ${panels}
    <script>
      document.querySelectorAll('.tabs [data-tab]').forEach((b)=>{ b.onclick=()=>{
        document.querySelectorAll('.tabs [data-tab]').forEach((x)=>x.classList.toggle('active', x===b));
        document.querySelectorAll('.tab-panel').forEach((p)=>p.classList.toggle('active', p.dataset.panel===b.dataset.tab));
      };});
      document.querySelectorAll('[data-issuer]').forEach((b)=>{ b.onclick=()=>{
        const fmt=b.closest('.tab-panel').querySelector('.fmt').value;
        location.href='/issuer/login?issuer='+encodeURIComponent(b.dataset.issuer)+'&format='+encodeURIComponent(fmt);
      };});
    </script>`;
  return shell('issuer', 'Issuer Portal', body);
}

app.get(['/issuer', '/issuer/'], async (_req, res) => {
  res.send(await renderIssuerPage(ISSUERS[0].id, null));
});

app.get(['/verifier', '/verifier/'], async (_req, res) => {
  const activeId = VERIFIERS[0].id;
  const verifiers = await Promise.all(
    VERIFIERS.map(async (v) => ({ ...v, sdJwtVctValues: await resolveSdJwtVctValues(v.typeNames) })),
  );
  const tabs = VERIFIERS.map((v) => `<button data-tab="${v.id}" class="${v.id === activeId ? 'active' : ''}">${v.tab || v.name}</button>`).join('');
  const panels = VERIFIERS.map((v) => verifierPanel(v, activeId)).join('');
  const body = `<div class="page-head"><h1>Verifier Portal</h1>
    <p>Pick a verification service tab, choose a format, then request proof. The QR &amp; result appear beside the verifier.</p></div>
    <div class="tabs">${tabs}</div>
    ${panels}
    <script>
      document.querySelectorAll('.tabs [data-tab]').forEach((b)=>{ b.onclick=()=>{
        document.querySelectorAll('.tabs [data-tab]').forEach((x)=>x.classList.toggle('active', x===b));
        document.querySelectorAll('.tab-panel').forEach((p)=>p.classList.toggle('active', p.dataset.panel===b.dataset.tab));
      };});
    </script>
    <script>window.OID4VC_BASE=${JSON.stringify(OID4VC_PUBLIC)};window.VERIFIERS=${JSON.stringify(verifiers)};</script>
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
    <div class="foot">OID4VC demo · Age / Rural Credit / Education use cases · all 4 formats.<br/>Users (Passw0rd!): citizen.over18/under18 · farmer.male/female · learner.secondary/graduate/postgraduate.</div>
  </aside>
  <main class="main">${body}</main></body></html>`;
}


app.listen(PORT, () => {
  console.log(`Age Verification demo on ${SELF}`);
  console.log(`  Issuer Portal:   ${SELF}/issuer`);
  console.log(`  Verifier Portal: ${SELF}/verifier`);
  console.log(`  oid4vc: ${OID4VC_BASE} | keycloak: ${AUTH} (realm ${KC_REALM})`);
});
