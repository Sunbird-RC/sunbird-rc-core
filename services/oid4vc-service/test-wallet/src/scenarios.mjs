// Deployment-portable scenario resolution.
//
// The wallet's four format scenarios are pinned to three well-known SCHEMA
// NAMES (not hardcoded schema UUIDs, which differ per deployment). At runtime
// we read the issuer's own metadata and resolve each scenario's actual
// `credential_configuration_id` (plus the sd-jwt `vct` / mdoc `doctype`) by
// matching on schema name + format — so the exact same wallet works against
// local, the VM, or any stack seeded with these schemas (see test/seed.mjs).
import { getJSON } from './util.mjs';

export const W3C_NAME = 'Wallet Test W3C Credential';
export const SDJWT_NAME = 'Wallet Test SD-JWT Credential';
export const MDOC_NAME = 'Wallet Test mDL';

// Sample claims per scenario — must satisfy each seeded schema's required
// properties (see test/seed.mjs, which creates schemas with exactly these).
export const CLAIMS = {
  ldp_vc: { name: 'Alice LDP' },
  jwt_vc_json: { name: 'Alice JWT' },
  'vc+sd-jwt': { name: 'Alice SD-JWT', age_over_18: true },
  mso_mdoc: { given_name: 'Jane', family_name: 'Doe' },
};

// Resolve the four scenarios against a live issuer's metadata.
// Returns an array [{ name, configId, format, claims, dcql }].
export async function resolveScenarios(base) {
  const meta = await getJSON(
    `${base}/.well-known/openid-credential-issuer`,
    'discovery',
  );
  const supported =
    meta.credential_configurations_supported || meta.credentials_supported || {};
  const entries = Object.entries(supported).map(([id, v]) => ({ id, ...v }));
  const find = (name, fmt) =>
    entries.find((e) => e.scope === name && e.format === fmt);

  const w3cLdp = find(W3C_NAME, 'ldp_vc');
  const w3cJwt = find(W3C_NAME, 'jwt_vc_json');
  const sd = find(SDJWT_NAME, 'vc+sd-jwt');
  const md = find(MDOC_NAME, 'mso_mdoc');

  const missing = [];
  if (!w3cLdp) missing.push(`"${W3C_NAME}" (ldp_vc)`);
  if (!w3cJwt) missing.push(`"${W3C_NAME}" (jwt_vc_json)`);
  if (!sd) missing.push(`"${SDJWT_NAME}" (vc+sd-jwt)`);
  if (!md) missing.push(`"${MDOC_NAME}" (mso_mdoc)`);
  if (missing.length) {
    throw new Error(
      `Issuer is missing wallet test schema(s): ${missing.join(', ')}. ` +
        `Seed them with: node test/seed.mjs (see README).`,
    );
  }

  return [
    {
      name: 'ldp_vc',
      configId: w3cLdp.id,
      format: 'ldp_vc',
      claims: CLAIMS.ldp_vc,
      dcql: { credentials: [{ id: 'c', format: 'ldp_vc', claims: [{ path: ['credentialSubject', 'name'] }] }] },
    },
    {
      name: 'jwt_vc_json',
      configId: w3cJwt.id,
      format: 'jwt_vc_json',
      claims: CLAIMS.jwt_vc_json,
      dcql: { credentials: [{ id: 'c', format: 'jwt_vc_json', claims: [{ path: ['credentialSubject', 'name'] }] }] },
    },
    {
      name: 'vc+sd-jwt',
      configId: sd.id,
      format: 'vc+sd-jwt',
      claims: CLAIMS['vc+sd-jwt'],
      dcql: {
        credentials: [
          { id: 'c', format: 'vc+sd-jwt', meta: { vct_values: [sd.vct] }, claims: [{ path: ['name'] }, { path: ['age_over_18'] }] },
        ],
      },
    },
    {
      name: 'mso_mdoc',
      configId: md.id,
      format: 'mso_mdoc',
      claims: CLAIMS.mso_mdoc,
      dcql: {
        credentials: [
          { id: 'c', format: 'mso_mdoc', meta: { doctype_value: md.doctype }, claims: [{ path: ['org.iso.18013.5.1', 'given_name'] }, { path: ['org.iso.18013.5.1', 'family_name'] }] },
        ],
      },
    },
  ];
}
