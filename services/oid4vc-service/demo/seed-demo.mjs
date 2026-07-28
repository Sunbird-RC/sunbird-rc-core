// Seeds ALL demo credential schemas + their issuer DIDs, idempotently:
//   1. National Identity Credential  (vc+sd-jwt)            — Use Case 1
//   2. Farmer Credential             (all 4 formats)        — Use Case 2
//   3. Academic Credential × N insts (all 4 formats)        — Use Case 3
//
//   local: node seed-demo.mjs
//   VM:    SCHEMA_BASE=http://credential-schema:3333 IDENTITY_BASE=http://identity:3332 \
//          OID4VC_BASE=http://oid4vc-service:3400 node seed-demo.mjs
//
// Each all-4-format schema gets an `mdoc` block (docType/namespace) and an
// issuer DID. mso_mdoc needs a P-256 (JsonWebKey2020) verification method on
// the issuer DID; ldp_vc needs Ed25519. See WARM-UP note at the bottom.
import { webcrypto as crypto } from 'node:crypto';

const SCHEMA_BASE = process.env.SCHEMA_BASE || 'http://localhost:3333';
const IDENTITY_BASE = process.env.IDENTITY_BASE || 'http://localhost:3332';
const OID4VC_BASE = process.env.OID4VC_BASE || 'http://localhost:3400';

async function jpost(url, body) {
  const res = await fetch(url, {
    method: 'POST',
    headers: { 'content-type': 'application/json', accept: 'application/json' },
    body: JSON.stringify(body),
  });
  const text = await res.text();
  if (!res.ok) throw new Error(`POST ${url} -> ${res.status}: ${text}`);
  return text ? JSON.parse(text) : {};
}
async function jget(url) {
  const res = await fetch(url, { headers: { accept: 'application/json' } });
  const text = await res.text();
  if (!res.ok) throw new Error(`GET ${url} -> ${res.status}: ${text}`);
  return text ? JSON.parse(text) : {};
}

async function metaConfigs() {
  const meta = await jget(`${OID4VC_BASE}/.well-known/openid-credential-issuer`);
  return meta.credential_configurations_supported || meta.credentials_supported || {};
}
function hasScope(configs, name) {
  return Object.values(configs).some((c) => c.scope === name);
}

async function genDid(authorityName, keyPairType) {
  const body = { content: [{ alsoKnownAs: [authorityName], services: [], method: 'rcw', ...(keyPairType ? { keyPairType } : {}) }] };
  const res = await jpost(`${IDENTITY_BASE}/did/generate`, body);
  return res[0].id;
}

async function createSchema({ id, name, author, properties, required, formats, vct, mdoc, tags }) {
  const body = {
    schema: {
      type: 'https://w3c-ccg.github.io/vc-json-schemas/',
      version: '1.0.0',
      id,
      name,
      author,
      authored: '2026-01-01T00:00:00.000Z',
      schema: {
        $id: `${id}-1.0`,
        $schema: 'https://json-schema.org/draft/2019-09/schema',
        description: name,
        type: 'object',
        properties,
        required,
        additionalProperties: true,
      },
    },
    tags: tags || ['oid4vc-demo'],
    status: 'PUBLISHED',
    oid4vciConfig: {
      oid4vciEnabled: true,
      oid4vciFormats: formats,
      ...(vct ? { vct } : {}),
      display: [{ name, locale: 'en-US' }],
      ...(mdoc ? { mdoc } : {}),
    },
  };
  const res = await jpost(`${SCHEMA_BASE}/credential-schema`, body);
  return res.schema.id;
}

const ALL4 = ['ldp_vc', 'jwt_vc_json', 'vc+sd-jwt', 'mso_mdoc'];

// --- ES256 warm-up --------------------------------------------------------
// mso_mdoc requires a P-256 (JsonWebKey2020) verification method on the issuer
// DID. identity-service adds one lazily the first time the DID signs a JWT /
// SD-JWT (ensureES256Key), but a fresh rcw DID whose FIRST issuance is mdoc
// fails. So we warm each all-4-format issuer by issuing one throwaway vc+sd-jwt,
// which provisions the ES256 key. Dependency-free: uses Node's global webcrypto
// for the holder key + PoP JWS. All calls go through OID4VC_BASE (internal).
const b64url = (buf) => Buffer.from(buf).toString('base64url');
const enc = (o) => b64url(JSON.stringify(o));

async function makeHolder() {
  const kp = await crypto.subtle.generateKey({ name: 'ECDSA', namedCurve: 'P-256' }, true, ['sign', 'verify']);
  const jwk = await crypto.subtle.exportKey('jwk', kp.publicKey);
  const publicJwk = { kty: 'EC', crv: 'P-256', x: jwk.x, y: jwk.y };
  const did = `did:jwk:${b64url(JSON.stringify(publicJwk))}`;
  return { priv: kp.privateKey, publicJwk, did };
}
async function popJwt(holder, audience, nonce) {
  const header = { alg: 'ES256', typ: 'openid4vci-proof+jwt', jwk: holder.publicJwk };
  const payload = { nonce, iss: holder.did, aud: audience, iat: Math.floor(Date.now() / 1000) };
  const input = `${enc(header)}.${enc(payload)}`;
  const sig = await crypto.subtle.sign({ name: 'ECDSA', hash: 'SHA-256' }, holder.priv, new TextEncoder().encode(input));
  return `${input}.${b64url(new Uint8Array(sig))}`;
}
// Rewrite any URL to go through OID4VC_BASE (offers embed the public URL).
const internal = (url) => url.replace(/^https?:\/\/[^/]+/, OID4VC_BASE);

async function warmUp(sdJwtConfigId, claims) {
  const offer = await jpost(`${OID4VC_BASE}/oid4vc/offer`, { credential_configuration_id: sdJwtConfigId, format: 'vc+sd-jwt', claims });
  const uri = new URLSearchParams(offer.qr_data.split('?')[1]).get('credential_offer_uri');
  const off = await jget(internal(uri));
  const grant = (off.grants || {})['urn:ietf:params:oauth:grant-type:pre-authorized_code'] || {};
  const tokenReq = { grant_type: 'urn:ietf:params:oauth:grant-type:pre-authorized_code', 'pre-authorized_code': grant['pre-authorized_code'] };
  if (grant.tx_code || grant.user_pin_required) tokenReq.tx_code = '000000';
  const token = await jpost(`${OID4VC_BASE}/oid4vc/token`, tokenReq);
  const holder = await makeHolder();
  const jwt = await popJwt(holder, off.credential_issuer, token.c_nonce);
  const res = await fetch(`${OID4VC_BASE}/oid4vc/credential`, {
    method: 'POST',
    headers: { 'content-type': 'application/json', accept: 'application/json', authorization: `Bearer ${token.access_token}` },
    body: JSON.stringify({ proof: { proof_type: 'jwt', jwt } }),
  });
  if (!res.ok) throw new Error(`warm-up credential -> ${res.status}: ${(await res.text()).slice(0, 140)}`);
}

// Resolve the vc+sd-jwt config id for an already-seeded schema (by scope).
function sdJwtConfigId(configs, name) {
  const hit = Object.entries(configs).find(([, v]) => v.scope === name && v.format === 'vc+sd-jwt');
  return hit && hit[0];
}

const FARMER_WARM = { farmer_id: 'FRM-0', full_name: 'warmup' };
const ACADEMIC_WARM = { learner_id: 'L-0', full_name: 'warmup' };

// --- Use Case 2: Farmer Credential ---------------------------------------
async function seedFarmer(configs) {
  const NAME = 'Farmer Credential';
  if (hasScope(configs, NAME)) {
    console.log(`  ✓ "${NAME}" already present — skip create`);
    return { name: NAME, configId: sdJwtConfigId(configs, NAME), warm: FARMER_WARM };
  }
  const did = await genDid('Farmer Registry Authority');
  const id = await createSchema({
    id: 'farmer-credential',
    name: NAME,
    author: did,
    properties: {
      farmer_id: { type: 'string', description: 'Farmer Identity Number' },
      full_name: { type: 'string', description: 'Full Name' },
      gender: { type: 'string', description: 'Gender' },
      land_area_acres: { type: 'number', description: 'Land area (acres)' },
      ownership_type: { type: 'string', description: 'Ownership type (Owned/Leased)' },
      land_record_ref: { type: 'string', description: 'Land Record Reference' },
      primary_crop: { type: 'string', description: 'Primary crop' },
      farm_location: { type: 'string', description: 'Farm location' },
    },
    required: ['farmer_id', 'full_name'],
    formats: ALL4,
    vct: NAME,
    mdoc: { docType: 'in.gov.farmer.1', namespace: 'in.gov.farmer.1' },
    tags: ['farmer', 'rural-credit-demo'],
  });
  console.log(`  + created "${NAME}" -> ${id}  (issuer ${did})`);
  return { name: NAME, configId: `${id}_vc+sd-jwt`, warm: FARMER_WARM };
}

// --- Use Case 3: Academic Credential (per institution) --------------------
const INSTITUTIONS = [
  { key: 'secondary', schemaId: 'academic-secondary', name: 'Secondary School Certificate', institution: 'State Secondary Education Board' },
  { key: 'graduate', schemaId: 'academic-degree', name: 'University Degree', institution: 'State University' },
  { key: 'postgraduate', schemaId: 'academic-pg', name: 'Postgraduate Degree', institution: 'National Postgraduate Institute' },
];
const ACADEMIC_VCT = 'Academic Credential';

async function seedAcademic(configs) {
  const out = [];
  for (const inst of INSTITUTIONS) {
    if (hasScope(configs, inst.name)) {
      console.log(`  ✓ "${inst.name}" already present — skip create`);
      out.push({ name: inst.name, configId: sdJwtConfigId(configs, inst.name), warm: ACADEMIC_WARM });
      continue;
    }
    const did = await genDid(inst.institution);
    const id = await createSchema({
      id: inst.schemaId,
      name: inst.name,
      author: did,
      properties: {
        learner_id: { type: 'string', description: 'Learner Identity Number' },
        full_name: { type: 'string', description: 'Full Name' },
        institution_name: { type: 'string', description: 'Institution Name' },
        qualification: { type: 'string', description: 'Qualification' },
        programme: { type: 'string', description: 'Programme' },
        completion_date: { type: 'string', format: 'date', description: 'Completion Date' },
        academic_result: { type: 'string', description: 'Academic Result' },
      },
      required: ['learner_id', 'full_name'],
      formats: ALL4,
      // Shared vct + docType so one verifier DCQL matches any institution.
      vct: ACADEMIC_VCT,
      mdoc: { docType: 'edu.academic.1', namespace: 'edu.academic.1' },
      tags: ['academic', 'education-demo', inst.key],
    });
    console.log(`  + created "${inst.name}" (${inst.institution}) -> ${id}  (issuer ${did})`);
    out.push({ name: inst.name, configId: `${id}_vc+sd-jwt`, warm: ACADEMIC_WARM });
  }
  return out;
}

async function main() {
  console.log('Seeding demo credential schemas');
  console.log(`  SCHEMA_BASE=${SCHEMA_BASE}  IDENTITY_BASE=${IDENTITY_BASE}  OID4VC_BASE=${OID4VC_BASE}`);
  const configs = await metaConfigs();
  const farmer = await seedFarmer(configs);
  const academic = await seedAcademic(configs);

  // Warm up ES256 keys so mso_mdoc works regardless of issuance order.
  console.log('Warming up issuer ES256 keys (for mso_mdoc)…');
  for (const s of [farmer, ...academic]) {
    if (!s?.configId) { console.log(`  ! ${s?.name}: no sd-jwt config found, skip warm-up`); continue; }
    try { await warmUp(s.configId, s.warm); console.log(`  ✓ warmed ${s.name}`); }
    catch (e) { console.log(`  ! warm-up ${s.name} failed: ${e.message}`); }
  }
  console.log('Done.');
}

main().catch((e) => { console.error('SEED FAILED:', e.message); process.exit(1); });
