// End-to-end test for all 3 use cases across formats, driving the REAL portal
// OIDC flow: Keycloak login (per issuer + format) -> issuer portal issues ->
// wallet accepts -> verifier requests a subset -> wallet presents -> assert
// what was disclosed (selective disclosure for vc+sd-jwt / mso_mdoc).
import { getJSON, postJSON } from '../test-wallet/src/util.mjs';
import { createHolder } from '../test-wallet/src/wallet-core.mjs';
import { acceptOffer } from '../test-wallet/src/vci-client.mjs';
import { present } from '../test-wallet/src/vp-client.mjs';
import { CredentialStore } from '../test-wallet/src/store.mjs';

const PORTAL = process.env.PORTAL_BASE || 'http://localhost:4000';
const OID4VC = process.env.OID4VC_BASE || 'http://localhost:3400';

function jar() {
  const s = {};
  return {
    header: () => Object.entries(s).map(([k, v]) => `${k}=${v}`).join('; '),
    absorb: (r) => { for (const c of (r.headers.getSetCookie?.() || [])) { const [kv] = c.split(';'); const i = kv.indexOf('='); s[kv.slice(0, i)] = kv.slice(i + 1); } },
  };
}
async function noRedirect(url, opts = {}, c) {
  const r = await fetch(url, { ...opts, redirect: 'manual', headers: { ...(opts.headers || {}), ...(c ? { cookie: c.header() } : {}) } });
  if (c) c.absorb(r);
  return r;
}

async function issueViaPortal(username, password, issuerId, format) {
  const c = jar();
  let r = await noRedirect(`${PORTAL}/issuer/authorize?issuer=${issuerId}&format=${encodeURIComponent(format)}`, {}, c);
  const authUrl = r.headers.get('location');
  r = await fetch(authUrl, { headers: { cookie: c.header() } }); c.absorb(r);
  const html = await r.text();
  const m = html.match(/action="([^"]*)"/);
  if (!m) throw new Error('KC login form not found');
  r = await noRedirect(m[1].replace(/&amp;/g, '&'), {
    method: 'POST', headers: { 'content-type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ username, password }),
  }, c);
  const cb = r.headers.get('location');
  if (!cb || !cb.includes('/callback')) throw new Error(`no callback (got ${r.status})`);
  const page = await (await fetch(cb, { headers: { cookie: c.header() } })).text();
  const offer = page.match(/openid-credential-offer:\/\/[^<\s"]+/);
  if (!offer) throw new Error('no offer on issued page: ' + page.slice(0, 200));
  return offer[0].replace(/&amp;/g, '&');
}

function buildDcql(v, format) {
  const req = v.request;
  if (format === 'mso_mdoc') return { credentials: [{ id: 'q', format, meta: { doctype_value: v.docType }, claims: req.map((c) => ({ path: [v.namespace, c] })) }] };
  if (format === 'vc+sd-jwt') return { credentials: [{ id: 'q', format, meta: { vct_values: v.typeNames }, claims: req.map((c) => ({ path: [c] })) }] };
  return { credentials: [{ id: 'q', format, meta: { type_values: v.typeNames.map((t) => ['VerifiableCredential', t]) }, claims: req.map((c) => ({ path: ['credentialSubject', c] })) }] };
}

const VERIF = {
  age: { vct: 'National Identity Credential', docType: null, namespace: null, typeNames: ['National Identity Credential'], request: ['over_18'], successKey: 'over_18' },
  rural: { vct: 'Farmer Credential', docType: 'in.gov.farmer.1', namespace: 'in.gov.farmer.1', typeNames: ['Farmer Credential'], request: ['land_area_acres', 'ownership_type', 'land_record_ref', 'primary_crop'] },
  edu: { vct: 'Academic Credential', docType: 'edu.academic.1', namespace: 'edu.academic.1', typeNames: ['Secondary School Certificate', 'University Degree', 'Postgraduate Degree'], request: ['qualification', 'programme', 'academic_result', 'institution_name'] },
};

const CASES = [
  { label: 'UC1 Age (sd-jwt)', user: 'citizen.over18', issuer: 'national-id', format: 'vc+sd-jwt', v: VERIF.age, private: ['date_of_birth', 'full_name', 'national_id', 'gender'] },
  { label: 'UC2 Rural Credit (sd-jwt)', user: 'farmer.male', issuer: 'farmer', format: 'vc+sd-jwt', v: VERIF.rural, private: ['farmer_id', 'full_name', 'gender', 'farm_location'] },
  { label: 'UC2 Rural Credit (mso_mdoc)', user: 'farmer.female', issuer: 'farmer', format: 'mso_mdoc', v: VERIF.rural, private: ['farmer_id', 'full_name', 'gender', 'farm_location'] },
  { label: 'UC3 Education (sd-jwt)', user: 'learner.graduate', issuer: 'edu-university', format: 'vc+sd-jwt', v: VERIF.edu, private: ['learner_id', 'full_name', 'completion_date'] },
  { label: 'UC3 Education (mso_mdoc)', user: 'learner.postgraduate', issuer: 'edu-pg', format: 'mso_mdoc', v: VERIF.edu, private: ['learner_id', 'full_name', 'completion_date'] },
];

function flatten(claims, namespace) {
  const q = claims?.q || (claims && Object.values(claims)[0]) || {};
  if (!namespace) return q;
  if (q[namespace] && typeof q[namespace] === 'object') return q[namespace];
  // mdoc dotted keys: "<namespace>.<element>"
  const out = {};
  for (const [k, v] of Object.entries(q)) out[k.startsWith(namespace + '.') ? k.slice(namespace.length + 1) : k] = v;
  return out;
}

async function run(c) {
  const offer = await issueViaPortal(c.user, 'Passw0rd!', c.issuer, c.format);
  const holder = await createHolder();
  const store = new CredentialStore();
  store.add(await acceptOffer(offer, holder));
  const req = await postJSON(`${OID4VC}/vp/request`, { dcql_query: buildDcql(c.v, c.format) }, {}, 'vp req');
  await present(req.qr_data, holder, store);
  const status = await getJSON(`${OID4VC}/vp/status/${req.transaction_id}`, 'status');
  const disclosed = flatten(status.claims, c.v.namespace);
  const leaked = c.private.filter((k) => k in disclosed);
  const gotRequested = c.v.request.every((k) => k in disclosed);
  const okVerified = status.verified === true;
  const pass = okVerified && gotRequested && leaked.length === 0;
  console.log(`\n${c.label}  [${c.user}]`);
  console.log(`  verified=${okVerified}  disclosed=${JSON.stringify(Object.keys(disclosed))}`);
  console.log(`  requested present=${gotRequested}  leaked=${leaked.length ? leaked.join(',') : 'none'}`);
  console.log(`  ${pass ? '✅ PASS' : '❌ FAIL'}`);
  return pass;
}

async function main() {
  let all = true;
  for (const c of CASES) { try { all = (await run(c)) && all; } catch (e) { console.log(`\n${c.label}\n  ❌ ${e.message}`); all = false; } }
  console.log(`\n──────── ${all ? '✅ ALL USE CASES PASSED' : '❌ SOME FAILED'} ────────`);
  if (!all) process.exit(1);
}
main().catch((e) => { console.error('❌', e.message); process.exit(1); });
