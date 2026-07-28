// Verifies the production-like wallet account flow (Part A) headlessly,
// replicating exactly what the browser client does:
//   signup -> generate holder -> encrypt privateJwk client-side -> upload
//   -> issue + persist a credential -> "re-login" -> decrypt -> SAME DID,
//   credentials restored -> reset identity -> holder + creds cleared.
import { webcrypto } from 'node:crypto';
globalThis.crypto ??= webcrypto;
import { createHolder, holderFromPrivateJwk } from '../test-wallet/src/wallet-core.mjs';
import { acceptOffer } from '../test-wallet/src/vci-client.mjs';
import { encryptJson, decryptJson } from '../test-wallet/web/crypto.js';

const WB = (process.env.WB || 'http://localhost:4100') + '/wallet-api';
const OID4VC = process.env.OID4VC_BASE || 'http://localhost:3400';
const USER = 'persist_test_user';
const PASS = 'sup3rsecret!';

function apiFactory(token) {
  return async (method, path, body) => {
    const r = await fetch(WB + path, {
      method,
      headers: { 'content-type': 'application/json', ...(token ? { authorization: `Bearer ${token}` } : {}) },
      body: body ? JSON.stringify(body) : undefined,
    });
    if (!r.ok) throw new Error(`${method} ${path} -> ${r.status}: ${(await r.text()).slice(0, 120)}`);
    return r.json();
  };
}

async function authToken(mode, api0) {
  const r = await api0('POST', `/${mode}`, { username: USER, password: PASS });
  return r.token;
}

async function issueOne(holder) {
  // Issue a Farmer Credential (sd-jwt) to persist.
  const meta = await (await fetch(`${OID4VC}/.well-known/openid-credential-issuer`)).json();
  const hit = Object.entries(meta.credential_configurations_supported || {}).find(
    ([, v]) => v.scope === 'Farmer Credential' && v.format === 'vc+sd-jwt',
  );
  if (!hit) throw new Error('Farmer Credential (vc+sd-jwt) not seeded — run seed-demo.mjs');
  const offer = await (await fetch(`${OID4VC}/oid4vc/offer`, {
    method: 'POST', headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ credential_configuration_id: hit[0], format: 'vc+sd-jwt', claims: { farmer_id: 'FRM-9', full_name: 'Test Farmer' } }),
  })).json();
  return acceptOffer(offer.qr_data, holder);
}

async function main() {
  const anon = apiFactory(null);
  // signup (or login if the user already exists from a prior run)
  let token;
  try { token = await authToken('signup', anon); console.log('signup OK'); }
  catch { token = await authToken('login', anon); console.log('login OK (existing user)'); }
  let api = apiFactory(token);

  // Clean slate for a deterministic run.
  await api('DELETE', '/holder');

  // First session: create + encrypt + upload holder.
  const holder1 = await createHolder();
  const enc = await encryptJson(holder1.privateJwk, PASS);
  await api('PUT', '/holder', { enc_holder: enc, did: holder1.did });
  const did1 = holder1.did;
  console.log(`session 1: created holder ${did1.slice(0, 42)}…`);

  // Issue + persist a credential.
  const rec = await issueOne(holder1);
  const saved = await api('POST', '/credentials', rec);
  console.log(`session 1: issued + persisted ${rec.format} (id=${saved.id})`);

  // Second session ("re-login" on another device): fetch + decrypt.
  const token2 = await authToken('login', anon);
  const api2 = apiFactory(token2);
  const h = await api2('GET', '/holder');
  const holder2 = holderFromPrivateJwk(await decryptJson(h.enc_holder, PASS));
  const did2 = holder2.did;
  const creds = await api2('GET', '/credentials');
  console.log(`session 2: restored holder ${did2.slice(0, 42)}…  credentials=${creds.length}`);

  // Assertions
  if (did1 !== did2) throw new Error(`DID NOT STABLE: ${did1} != ${did2}`);
  if (!creds.some((c) => c.format === 'vc+sd-jwt')) throw new Error('credential not restored');

  // Reset identity → holder + creds gone; new holder differs.
  await api2('DELETE', '/holder');
  const afterReset = await api2('GET', '/holder');
  const credsAfter = await api2('GET', '/credentials');
  if (afterReset.enc_holder !== null) throw new Error('holder not cleared on reset');
  if (credsAfter.length !== 0) throw new Error('credentials not cleared on reset');
  const holder3 = await createHolder();
  if (holder3.did === did1) throw new Error('reset did not change identity');

  console.log('\n✅ Account flow verified:');
  console.log('   • stable DID across re-login (same key restored)');
  console.log('   • credentials persisted + restored per account');
  console.log('   • reset clears holder + credentials and yields a new DID');
}
main().catch((e) => { console.error('❌', e.message); process.exit(1); });
