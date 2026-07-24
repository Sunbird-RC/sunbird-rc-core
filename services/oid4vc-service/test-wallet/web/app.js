// Modern wallet UI. Reuses the SAME core modules as the headless e2e runner —
// only the views/rendering are browser-specific.
import QRCode from 'qrcode';
import { getJSON, postJSON } from '../src/util.mjs';
import { createHolder } from '../src/wallet-core.mjs';
import { acceptOffer } from '../src/vci-client.mjs';
import { present, resolveRequest } from '../src/vp-client.mjs';
import { CredentialStore } from '../src/store.mjs';
import { resolveScenarios } from '../src/scenarios.mjs';

const $ = (id) => document.getElementById(id);
const store = new CredentialStore();
let holder;
let lastVpTxn = null;

// scenarios resolved live from issuer metadata (portable across deployments)
let SCENARIOS = {};
let scenariosBase = null;
async function ensureScenarios() {
  const b = base();
  if (scenariosBase === b && Object.keys(SCENARIOS).length) return SCENARIOS;
  const arr = await resolveScenarios(b);
  SCENARIOS = Object.fromEntries(arr.map((s) => [s.format, s]));
  scenariosBase = b;
  return SCENARIOS;
}

const base = () => $('base').value.replace(/\/$/, '');

function log(msg) {
  const el = $('log');
  el.textContent += `${new Date().toLocaleTimeString()}  ${msg}\n`;
  el.scrollTop = el.scrollHeight;
}
let toastTimer;
function toast(msg) {
  const t = $('toast'); t.textContent = msg; t.classList.add('show');
  clearTimeout(toastTimer); toastTimer = setTimeout(() => t.classList.remove('show'), 2600);
}

// --- views -----------------------------------------------------------------
const TITLES = { credentials: 'Credentials', receive: 'Receive', present: 'Present', activity: 'Activity', settings: 'Settings' };
function switchView(name) {
  document.querySelectorAll('.view').forEach((v) => (v.hidden = v.dataset.view !== name));
  document.querySelectorAll('.nav button').forEach((b) => b.classList.toggle('active', b.dataset.view === name));
  $('view-title').textContent = TITLES[name] || name;
}

// --- holder ----------------------------------------------------------------
async function newHolder() {
  holder = await createHolder();
  $('side-did').textContent = holder.did;
  $('did-short').textContent = holder.did.slice(0, 22) + '…';
  $('full-did').textContent = holder.did;
  log(`holder identity ready · ${holder.did.slice(0, 42)}…`);
}

// --- credential display ----------------------------------------------------
const ENVELOPE = new Set(['iss', 'sub', 'iat', 'exp', 'nbf', 'vct', 'jti', 'cnf', '_sd', '_sd_alg', 'id',
  '@context', 'type', 'issuer', 'issuanceDate', 'expirationDate', 'proof', 'credentialSchema', 'credentialStatus']);
const GRAD = { ldp_vc: 'g-ldp', jwt_vc_json: 'g-jwt', 'vc+sd-jwt': 'g-sdjwt', mso_mdoc: 'g-mdoc' };

function humanType(c) {
  if (c.vct) return c.vct;
  if (c.docType) return c.docType.split('.').pop();
  if (Array.isArray(c.claims?.type)) return c.claims.type[c.claims.type.length - 1];
  return c.label || c.configId || 'Credential';
}
function subjectClaims(c) {
  // mdoc claims are {namespace:{el:val}} — flatten one level for display.
  let claims = c.claims || {};
  if (c.docType && Object.values(claims).every((v) => v && typeof v === 'object')) {
    claims = Object.assign({}, ...Object.values(claims));
  }
  const out = {};
  for (const [k, v] of Object.entries(claims)) if (!ENVELOPE.has(k)) out[k] = v;
  return out;
}
function shortIssuer(iss) { return iss ? iss.replace(/^did:[a-z]+:/, '').slice(0, 12) + '…' : '—'; }

function renderCreds() {
  const el = $('creds');
  const items = store.all();
  if (!items.length) {
    el.innerHTML = `<div class="empty"><div class="big">🪪</div>
      <p>No credentials yet.<br/>Go to <b>Receive</b> to add one.</p>
      <button class="btn" onclick="document.querySelector('[data-view=receive]').click()">📥 Receive a credential</button></div>`;
    return;
  }
  el.innerHTML = '';
  items.forEach((c) => {
    const claims = subjectClaims(c);
    const keys = Object.keys(claims).slice(0, 2);
    const card = document.createElement('div');
    card.className = `pass ${GRAD[c.format] || 'g-ldp'}`;
    card.innerHTML = `<span class="fmt">${c.format}</span>
      <h3>${humanType(c)}</h3>
      <div class="issuer">Issued by ${shortIssuer(c.issuer)}</div>
      <div class="keyclaims">${keys.map((k) => `<div class="kc">${k}<b>${fmtVal(claims[k])}</b></div>`).join('') || '<div class="kc">tap for details</div>'}</div>`;
    card.onclick = () => openDetail(c);
    el.appendChild(card);
  });
}
function fmtVal(v) { return typeof v === 'boolean' ? (v ? 'Yes' : 'No') : String(v); }

// --- detail modal ----------------------------------------------------------
let detailCred = null;
function openDetail(c) {
  detailCred = c;
  $('detail-title').textContent = humanType(c);
  $('detail-sub').textContent = `${c.format} · issued by ${c.issuer || '—'}`;
  const claims = subjectClaims(c);
  $('detail-kv').innerHTML = Object.entries(claims).map(([k, v]) =>
    `<span class="k">${k}</span><span class="v">${fmtVal(v)}</span>`).join('') || '<span class="k">(no claims)</span><span></span>';
  $('detail-modal').classList.add('show');
}
function closeDetail() { $('detail-modal').classList.remove('show'); detailCred = null; }

// --- consent modal (returns a promise) -------------------------------------
function askConsent(verifier, attrs) {
  $('consent-verifier').textContent = verifier;
  $('consent-attrs').innerHTML = (attrs.length ? attrs : ['(entire credential)'])
    .map((a) => `<div class="a">✓ ${a}</div>`).join('');
  const m = $('consent-modal'); m.classList.add('show');
  return new Promise((resolve) => {
    const done = (v) => { m.classList.remove('show'); $('consent-ok').onclick = null; $('consent-cancel').onclick = null; resolve(v); };
    $('consent-ok').onclick = () => done(true);
    $('consent-cancel').onclick = () => done(false);
  });
}

// --- QR --------------------------------------------------------------------
async function renderQr(where, text) {
  const el = $(where); el.innerHTML = '';
  if (!text) return;
  const canvas = document.createElement('canvas');
  await QRCode.toCanvas(canvas, text, { width: 190, margin: 1 });
  el.appendChild(canvas);
  const s = document.createElement('div'); s.className = 'link'; s.textContent = text; el.appendChild(s);
}

// --- issuance --------------------------------------------------------------
async function acceptOfferLink(link) {
  if (!link) return toast('Paste an offer link first');
  try {
    log('receiving credential…');
    const rec = await acceptOffer(link, holder);
    store.add(rec); renderCreds();
    log(`✅ received ${rec.format} (${humanType(rec)})`);
    toast(`✅ ${humanType(rec)} added`);
    switchView('credentials');
  } catch (e) { log(`❌ receive failed: ${e.message}`); toast('❌ ' + e.message); }
}
async function quickIssue(format) {
  try {
    const sc = (await ensureScenarios())[format];
    log(`creating ${format} test offer…`);
    const offer = await postJSON(`${base()}/oid4vc/offer`, { credential_configuration_id: sc.configId, format, claims: sc.claims });
    await renderQr('offer-qr', offer.qr_data);
    $('offer-link').value = offer.qr_data;
    await acceptOfferLink(offer.qr_data);
  } catch (e) { log(`❌ ${format} offer failed: ${e.message}`); toast('❌ ' + e.message); }
}

// --- presentation ----------------------------------------------------------
async function presentLink(link) {
  if (!link) return toast('Paste a request link first');
  try {
    const reqObj = await resolveRequest(link);
    const cq = (reqObj.dcql_query?.credentials || [])[0] || {};
    const attrs = (cq.claims || []).map((c) => (c.path || []).join('.')).filter(Boolean);
    const verifier = reqObj.client_id ? `Verifier: ${reqObj.client_id}` : 'A verifier is requesting proof.';
    const ok = await askConsent(verifier, attrs);
    if (!ok) { log('❌ presentation cancelled (no consent)'); return; }

    const attrList = attrs.join(', ') || 'the whole credential';
    log(`presenting — disclosing only: ${attrList}…`);
    const { presented } = await present(link, holder, store);
    log(`✅ presented ${presented.format} — disclosed only: ${attrList}`);
    const q = new URLSearchParams(link.substring(link.indexOf('?') + 1));
    const txn = lastVpTxn || (q.get('request_uri') || '').split('/').pop();
    if (txn) { const status = await getJSON(`${base()}/vp/status/${txn}`); showResult(status); log(`verifier: verified=${status.verified}`); }
    toast('✅ Presented');
  } catch (e) { log(`❌ present failed: ${e.message}`); showResult({ verified: false, error: e.message, checks: {} }); toast('❌ ' + e.message); }
}
async function quickVp(format) {
  try {
    const sc = (await ensureScenarios())[format];
    log(`creating ${format} test request…`);
    const req = await postJSON(`${base()}/vp/request`, { dcql_query: sc.dcql });
    lastVpTxn = req.transaction_id;
    await renderQr('vp-qr', req.qr_data);
    $('vp-link').value = req.qr_data;
    await presentLink(req.qr_data);
  } catch (e) { log(`❌ ${format} VP failed: ${e.message}`); toast('❌ ' + e.message); }
}
function showResult(status) {
  const el = $('vp-result');
  const ok = status.verified;
  const checks = status.checks || {};
  el.className = `result show ${ok ? 'ok' : 'fail'}`;
  el.innerHTML = `<div class="verdict">${ok ? '✅ VERIFIED' : '❌ NOT VERIFIED'}</div>
    <div class="checks">${Object.entries(checks).map(([k, v]) => `<span class="chk ${v === 'OK' ? '' : 'bad'}">${k}: ${v}</span>`).join('')}</div>
    ${status.claims ? `<pre>${JSON.stringify(status.claims, null, 2)}</pre>` : ''}
    ${status.error ? `<div style="color:var(--danger)">${status.error}</div>` : ''}`;
}

async function copy(text, label) {
  try { await navigator.clipboard.writeText(text); toast((label || 'Copied') + ' ✓'); }
  catch { toast('Copy failed — select manually'); }
}

// --- wire up ---------------------------------------------------------------
function init() {
  $('base').value = location.port === '5555' ? `${location.protocol}//${location.hostname}:3400` : location.origin;
  newHolder();

  document.querySelectorAll('.nav button').forEach((b) => (b.onclick = () => switchView(b.dataset.view)));
  $('accept-offer').onclick = () => acceptOfferLink($('offer-link').value.trim());
  $('present').onclick = () => presentLink($('vp-link').value.trim());
  document.querySelectorAll('.quick-issue').forEach((b) => (b.onclick = () => quickIssue(b.dataset.format)));
  document.querySelectorAll('.quick-vp').forEach((b) => (b.onclick = () => quickVp(b.dataset.format)));

  $('did-chip').onclick = () => copy(holder.did, 'DID copied');
  $('copy-did').onclick = () => copy(holder.did, 'DID copied');
  $('regen').onclick = () => { store.items = []; renderCreds(); newHolder(); toast('New identity generated'); switchView('credentials'); };
  $('save-base').onclick = () => { SCENARIOS = {}; scenariosBase = null; toast('Base URL applied'); };
  $('clear-log').onclick = () => { $('log').textContent = ''; };

  $('detail-close').onclick = closeDetail;
  $('detail-copy').onclick = () => detailCred && copy(typeof detailCred.raw === 'string' ? detailCred.raw : JSON.stringify(detailCred.raw), 'Raw copied');
  $('detail-delete').onclick = () => { if (detailCred) { store.items = store.items.filter((x) => x.id !== detailCred.id); renderCreds(); closeDetail(); toast('Credential deleted'); } };
  document.querySelectorAll('.modal').forEach((m) => (m.onclick = (e) => { if (e.target === m) m.classList.remove('show'); }));

  renderCreds();
}
init();
