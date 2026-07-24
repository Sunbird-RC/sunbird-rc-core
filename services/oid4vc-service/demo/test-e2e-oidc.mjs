// Full Age Verification demo, automated end-to-end:
//   Keycloak OIDC login (citizen) -> Issuer Portal issues National Identity
//   credential -> wallet accepts -> Verifier requests ONLY over_18 -> wallet
//   presents with selective disclosure -> verifier PASS/FAIL.
// Proves the whole chain (auth + issuance + privacy-preserving presentation).
import { getJSON, postJSON } from '../test-wallet/src/util.mjs';
import { createHolder } from '../test-wallet/src/wallet-core.mjs';
import { acceptOffer } from '../test-wallet/src/vci-client.mjs';
import { present } from '../test-wallet/src/vp-client.mjs';
import { CredentialStore } from '../test-wallet/src/store.mjs';

const PORTAL = process.env.PORTAL_BASE || 'http://localhost:4000';
const OID4VC = process.env.OID4VC_BASE || 'http://localhost:3400';
const VCT = 'National Identity Credential';

// --- minimal cookie jar over fetch (for the Keycloak login form) ---
function jar() {
  const store = {};
  return {
    header: () => Object.entries(store).map(([k, v]) => `${k}=${v}`).join('; '),
    absorb: (res) => {
      const sc = res.headers.getSetCookie ? res.headers.getSetCookie() : [];
      for (const c of sc) { const [kv] = c.split(';'); const i = kv.indexOf('='); store[kv.slice(0, i)] = kv.slice(i + 1); }
    },
  };
}
async function noRedirect(url, opts = {}, cookies) {
  const res = await fetch(url, { ...opts, redirect: 'manual', headers: { ...(opts.headers || {}), ...(cookies ? { cookie: cookies.header() } : {}) } });
  if (cookies) cookies.absorb(res);
  return res;
}

// Drive the real OIDC auth-code login, returning the issuer portal's issued page HTML.
async function issueViaOidc(username, password) {
  const c = jar();
  // 1) /issuer/authorize -> 302 to Keycloak auth (browser uses /issuer/login,
  //    which first ends any SSO session then lands here; headless has no
  //    session so it goes straight to the auth redirect).
  let res = await noRedirect(`${PORTAL}/issuer/authorize`, {}, c);
  const authUrl = res.headers.get('location');
  // 2) GET KC login page (sets cookies), parse form action
  res = await fetch(authUrl, { headers: { cookie: c.header() } }); c.absorb(res);
  const html = await res.text();
  const m = html.match(/action="([^"]*)"/);
  if (!m) throw new Error('KC login form not found');
  const formAction = m[1].replace(/&amp;/g, '&');
  // 3) POST credentials -> 302 to /callback?code=...
  res = await noRedirect(formAction, {
    method: 'POST',
    headers: { 'content-type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ username, password }),
  }, c);
  const cb = res.headers.get('location');
  if (!cb || !cb.includes('/callback')) throw new Error(`login did not redirect to callback (got ${res.status} ${cb})`);
  // 4) GET /callback -> issuance; extract offer link
  const page = await (await fetch(cb, { headers: { cookie: c.header() } })).text();
  const offer = page.match(/openid-credential-offer:\/\/[^<\s"]+/);
  if (!offer) throw new Error('no offer on issued page:\n' + page.slice(0, 400));
  return offer[0].replace(/&amp;/g, '&');
}

async function run(label, username, password) {
  console.log(`\n=== ${label} (${username}) ===`);
  const offerLink = await issueViaOidc(username, password);
  console.log(`  OIDC login OK; issuer portal created offer`);

  const holder = await createHolder();
  const store = new CredentialStore();
  const rec = await acceptOffer(offerLink, holder);
  store.add(rec);
  console.log(`  wallet stored credential; holds: ${JSON.stringify(Object.keys(rec.claims).filter(k => ['national_id','full_name','date_of_birth','gender','over_18'].includes(k)))}`);

  // Verifier asks only over_18
  const req = await postJSON(`${OID4VC}/vp/request`, {
    dcql_query: { credentials: [{ id: 'age', format: 'vc+sd-jwt', meta: { vct_values: [VCT] }, claims: [{ path: ['over_18'] }] }] },
  }, {}, 'vp request');
  await present(req.qr_data, holder, store);
  const status = await getJSON(`${OID4VC}/vp/status/${req.transaction_id}`, 'status');

  const disclosed = status.claims?.age || {};
  const leaked = ['date_of_birth', 'full_name', 'national_id', 'gender'].filter((k) => k in disclosed);
  if (leaked.length) throw new Error(`PRIVACY LEAK: ${leaked.join(', ')}`);
  const pass = status.verified && disclosed.over_18 === true;
  console.log(`  disclosed to verifier: ${JSON.stringify(disclosed)}  (nothing else)`);
  console.log(`  verdict: ${pass ? '✅ ACCESS GRANTED (over 18)' : '⛔ ACCESS DENIED (not over 18)'}`);
  return { over18: disclosed.over_18, leaked: leaked.length };
}

async function main() {
  const a = await run('Citizen ABOVE 18', 'citizen.over18', 'Passw0rd!');
  const b = await run('Citizen BELOW 18', 'citizen.under18', 'Passw0rd!');
  console.log('\n──────── SUMMARY ────────');
  console.log(`  citizen.over18  -> over_18=${a.over18}  leaked=${a.leaked}  => PASS`);
  console.log(`  citizen.under18 -> over_18=${b.over18}  leaked=${b.leaked}  => FAIL`);
  if (a.over18 !== true || b.over18 !== false || a.leaked || b.leaked) { console.error('❌ unexpected'); process.exit(1); }
  console.log('\n✅ Full demo verified: Keycloak auth → issuance → over_18-only selective disclosure.');
}
main().catch((e) => { console.error('❌', e.message); process.exit(1); });
