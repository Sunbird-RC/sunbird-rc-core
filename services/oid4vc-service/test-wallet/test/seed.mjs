// One-time (idempotent) seeder: creates the three well-known schemas the test
// wallet's scenarios resolve against (by name), across ALL FOUR formats:
//   - "Wallet Test W3C Credential"  -> ldp_vc + jwt_vc_json
//   - "Wallet Test SD-JWT Credential" -> vc+sd-jwt
//   - "Wallet Test mDL"             -> mso_mdoc (needs an EC P-256 issuer DID)
//
// Portable across deployments — point it at any stack:
//   local:  node test/seed.mjs
//   VM:     SCHEMA_BASE=http://<host>/credential-schema IDENTITY_BASE=http://<host>/did \
//             OID4VC_BASE=http://<host> node test/seed.mjs
//
// Idempotent: if a schema with the target name+format already exists (per
// GET /credential-schema/oid4vci-configs, read via OID4VC_BASE discovery), it
// is skipped.
import { postJSON, getJSON } from '../src/util.mjs';
import { W3C_NAME, SDJWT_NAME, MDOC_NAME } from '../src/scenarios.mjs';

const SCHEMA_BASE = process.env.SCHEMA_BASE || 'http://localhost:3333';
const IDENTITY_BASE = process.env.IDENTITY_BASE || 'http://localhost:3332';
const OID4VC_BASE = process.env.OID4VC_BASE || 'http://localhost:3400';

async function existingConfigs() {
  const meta = await getJSON(`${OID4VC_BASE}/.well-known/openid-credential-issuer`, 'discovery');
  const supported = meta.credential_configurations_supported || meta.credentials_supported || {};
  return Object.values(supported); // each has scope (name) + format
}

function has(configs, name, fmt) {
  return configs.some((c) => c.scope === name && c.format === fmt);
}

async function genDid(keyPairType) {
  const body = { content: [{ alsoKnownAs: ['wallet-test-seed'], services: [], method: 'rcw', ...(keyPairType ? { keyPairType } : {}) }] };
  const res = await postJSON(`${IDENTITY_BASE}/did/generate`, body, {}, 'generate DID');
  return res[0].id;
}

async function createSchema(name, author, props, required, formats, extraOid4vci = {}) {
  const body = {
    schema: {
      type: 'https://w3c-ccg.github.io/vc-json-schemas/',
      version: '1.0.0',
      id: `wallet-test-${name.replace(/[^a-zA-Z0-9]+/g, '-').toLowerCase()}`,
      name,
      author,
      authored: '2026-01-01T00:00:00.000Z',
      schema: {
        $id: `${name.replace(/[^a-zA-Z0-9]+/g, '')}-1.0`,
        $schema: 'https://json-schema.org/draft/2019-09/schema',
        description: `Test-wallet seed schema: ${name}`,
        type: 'object',
        properties: props,
        required,
        additionalProperties: true,
      },
    },
    tags: ['wallet-test'],
    status: 'PUBLISHED',
    oid4vciConfig: {
      oid4vciEnabled: true,
      oid4vciFormats: formats,
      display: [{ name, locale: 'en-US' }],
      ...extraOid4vci,
    },
  };
  const res = await postJSON(`${SCHEMA_BASE}/credential-schema`, body, {}, `create schema ${name}`);
  return res.schema.id;
}

async function main() {
  console.log(`Seeding wallet test schemas:`);
  console.log(`  SCHEMA_BASE=${SCHEMA_BASE}\n  IDENTITY_BASE=${IDENTITY_BASE}\n  OID4VC_BASE=${OID4VC_BASE}`);
  const configs = await existingConfigs();

  // W3C (ldp_vc + jwt_vc_json)
  if (has(configs, W3C_NAME, 'ldp_vc') && has(configs, W3C_NAME, 'jwt_vc_json')) {
    console.log(`  ✓ "${W3C_NAME}" already present (ldp_vc, jwt_vc_json) — skip`);
  } else {
    const did = await genDid();
    const id = await createSchema(W3C_NAME, did, { name: { type: 'string' } }, ['name'], ['ldp_vc', 'jwt_vc_json']);
    console.log(`  + created "${W3C_NAME}" -> ${id}`);
  }

  // SD-JWT (vc+sd-jwt), vct == schema name
  if (has(configs, SDJWT_NAME, 'vc+sd-jwt')) {
    console.log(`  ✓ "${SDJWT_NAME}" already present (vc+sd-jwt) — skip`);
  } else {
    const did = await genDid();
    const id = await createSchema(
      SDJWT_NAME, did,
      { name: { type: 'string' }, age_over_18: { type: 'boolean' } }, ['name'],
      ['vc+sd-jwt'], { vct: SDJWT_NAME },
    );
    console.log(`  + created "${SDJWT_NAME}" -> ${id}`);
  }

  // mDL (mso_mdoc) — requires an EC P-256 (JsonWebKey2020) issuer DID
  if (has(configs, MDOC_NAME, 'mso_mdoc')) {
    console.log(`  ✓ "${MDOC_NAME}" already present (mso_mdoc) — skip`);
  } else {
    const did = await genDid('JsonWebKey2020');
    const id = await createSchema(
      MDOC_NAME, did,
      { given_name: { type: 'string' }, family_name: { type: 'string' } }, ['given_name'],
      ['mso_mdoc'], { mdoc: { docType: 'org.iso.18013.5.1.mDL', namespace: 'org.iso.18013.5.1' } },
    );
    console.log(`  + created "${MDOC_NAME}" -> ${id}`);
  }

  console.log('Seed complete.');
}

main().catch((e) => { console.error('SEED FAILED:', e.message); process.exit(1); });
