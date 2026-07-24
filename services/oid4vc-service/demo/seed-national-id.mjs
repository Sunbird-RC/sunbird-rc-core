// Seeds the "National Identity Credential" schema (vc+sd-jwt) used by the
// Age Verification demo, issued by a dedicated "National Identity Authority"
// DID. Idempotent: skips if a vc+sd-jwt config with this name already exists.
//
//   local: node seed-national-id.mjs
//   VM:    SCHEMA_BASE=http://<host> IDENTITY_BASE=http://<host> OID4VC_BASE=http://<host> node seed-national-id.mjs
//
// The credential carries: national_id, full_name, date_of_birth, gender,
// over_18 — every one selectively disclosable (see signSdJwtVc default), so the
// holder can later present ONLY over_18.
const SCHEMA_BASE = process.env.SCHEMA_BASE || 'http://localhost:3333';
const IDENTITY_BASE = process.env.IDENTITY_BASE || 'http://localhost:3332';
const OID4VC_BASE = process.env.OID4VC_BASE || 'http://localhost:3400';

export const NATIONAL_ID_NAME = 'National Identity Credential';

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

async function alreadySeeded() {
  const meta = await jget(`${OID4VC_BASE}/.well-known/openid-credential-issuer`);
  const supported = meta.credential_configurations_supported || meta.credentials_supported || {};
  return Object.values(supported).some((c) => c.scope === NATIONAL_ID_NAME && c.format === 'vc+sd-jwt');
}

async function main() {
  console.log(`Seeding "${NATIONAL_ID_NAME}" (vc+sd-jwt)`);
  console.log(`  SCHEMA_BASE=${SCHEMA_BASE}  IDENTITY_BASE=${IDENTITY_BASE}  OID4VC_BASE=${OID4VC_BASE}`);
  if (await alreadySeeded()) {
    console.log(`  ✓ already present — skip`);
    return;
  }
  // Dedicated issuer DID with a human-readable authority name.
  const did = (await jpost(`${IDENTITY_BASE}/did/generate`, {
    content: [{ alsoKnownAs: ['National Identity Authority'], services: [], method: 'rcw' }],
  }))[0].id;

  const body = {
    schema: {
      type: 'https://w3c-ccg.github.io/vc-json-schemas/',
      version: '1.0.0',
      id: 'national-identity-credential',
      name: NATIONAL_ID_NAME,
      author: did,
      authored: '2026-01-01T00:00:00.000Z',
      schema: {
        $id: 'NationalIdentityCredential-1.0',
        $schema: 'https://json-schema.org/draft/2019-09/schema',
        description: 'National Identity Credential issued by the National Identity Authority',
        type: 'object',
        properties: {
          national_id: { type: 'string', description: 'National Identity Number' },
          full_name: { type: 'string', description: 'Full Name' },
          date_of_birth: { type: 'string', format: 'date', description: 'Date of Birth (YYYY-MM-DD)' },
          gender: { type: 'string', description: 'Gender' },
          over_18: { type: 'boolean', description: 'True if the holder is 18 or older' },
        },
        required: ['national_id', 'full_name'],
        additionalProperties: true,
      },
    },
    tags: ['national-identity', 'age-verification-demo'],
    status: 'PUBLISHED',
    oid4vciConfig: {
      oid4vciEnabled: true,
      oid4vciFormats: ['vc+sd-jwt'],
      vct: NATIONAL_ID_NAME,
      display: [{ name: NATIONAL_ID_NAME, locale: 'en-US' }],
    },
  };
  const res = await jpost(`${SCHEMA_BASE}/credential-schema`, body);
  console.log(`  + created "${NATIONAL_ID_NAME}" -> ${res.schema.id}  (issuer ${did})`);
}

main().catch((e) => { console.error('SEED FAILED:', e.message); process.exit(1); });
