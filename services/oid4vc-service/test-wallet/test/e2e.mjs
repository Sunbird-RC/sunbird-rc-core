// End-to-end test: drives the full OID4VCI issuance + OID4VP (DCQL) presentation
// cycle for ALL FOUR formats against a live local stack, using the wallet core
// as an independent holder.
//
// Roles played here:
//   - issuer   : POST /oid4vc/offer (issuer-initiated offer, as the registry would)
//   - wallet   : the wallet core (accept offer, present) — the real subject under test
//   - verifier : POST /vp/request + GET /vp/status
//
// Run:  OID4VC_BASE=http://localhost:3400 node test/e2e.mjs
import { postJSON, getJSON } from '../src/util.mjs';
import { createHolder } from '../src/wallet-core.mjs';
import { acceptOffer } from '../src/vci-client.mjs';
import { present } from '../src/vp-client.mjs';
import { CredentialStore } from '../src/store.mjs';
import { resolveScenarios } from '../src/scenarios.mjs';

const OID4VC = process.env.OID4VC_BASE || 'http://localhost:3400';

// Scenarios are resolved at runtime from the issuer's metadata (by schema
// name + format) — portable across any stack seeded via test/seed.mjs. No
// hardcoded, deployment-specific schema UUIDs.

const EXPECTED_CHECKS = [
  'holderSignature',
  'nonce',
  'credentialSignatures',
  'holderBinding',
  'revocation',
  'dcql',
];

function log(...a) {
  console.log(...a);
}

async function runScenario(sc, holder, store) {
  log(`\n=== ${sc.name} ===`);

  // --- Issuance ---------------------------------------------------------
  // Issuer creates an offer (as the registry hook / issuer backend would).
  const offer = await postJSON(
    `${OID4VC}/oid4vc/offer`,
    { credential_configuration_id: sc.configId, format: sc.format, claims: sc.claims },
    {},
    'create offer',
  );
  log(`  offer created: ${offer.qr_data.slice(0, 64)}...`);

  // Wallet accepts the offer purely from the deep link.
  const record = await acceptOffer(offer.qr_data, holder);
  store.add(record);
  log(`  ✅ issued (${record.format}); holder-bound to ${holder.did.slice(0, 28)}...`);

  // --- Presentation -----------------------------------------------------
  // Verifier creates a DCQL request.
  const req = await postJSON(`${OID4VC}/vp/request`, { dcql_query: sc.dcql }, {}, 'vp request');
  log(`  vp request: ${req.qr_data.slice(0, 64)}...`);

  // Wallet presents.
  await present(req.qr_data, holder, store);

  // Verifier polls the result.
  const status = await getJSON(`${OID4VC}/vp/status/${req.transaction_id}`, 'vp status');

  // --- Assertions -------------------------------------------------------
  if (!status.verified) {
    throw new Error(`VP not verified: ${JSON.stringify(status)}`);
  }
  for (const c of EXPECTED_CHECKS) {
    if (status.checks?.[c] !== 'OK') {
      throw new Error(`check '${c}' != OK (got ${status.checks?.[c]}) — ${JSON.stringify(status.checks)}`);
    }
  }
  log(`  ✅ presented & verified — checks: ${EXPECTED_CHECKS.map((c) => `${c}:OK`).join(', ')}`);
  log(`  disclosed claims: ${JSON.stringify(status.claims)}`);
  return true;
}

async function main() {
  log(`OID4VC Test Wallet — end-to-end against ${OID4VC}`);

  // sanity: service up + discovery reachable; resolve scenarios by schema name
  const meta = await getJSON(`${OID4VC}/.well-known/openid-credential-issuer`, 'discovery');
  const supported = meta.credential_configurations_supported || meta.credentials_supported || {};
  log(`Issuer advertises ${Object.keys(supported).length} credential configurations.`);
  const SCENARIOS = await resolveScenarios(OID4VC);

  const holder = await createHolder();
  log(`Holder DID: ${holder.did}`);
  const store = new CredentialStore();

  const results = [];
  for (const sc of SCENARIOS) {
    try {
      await runScenario(sc, holder, store);
      results.push([sc.name, 'PASS']);
    } catch (err) {
      log(`  ❌ ${sc.name} FAILED: ${err.message}`);
      results.push([sc.name, 'FAIL']);
    }
  }

  log('\n──────── SUMMARY ────────');
  for (const [name, r] of results) log(`  ${r === 'PASS' ? '✅' : '❌'} ${name}: ${r}`);
  const failed = results.filter(([, r]) => r === 'FAIL').length;
  log(`\n${results.length - failed}/${results.length} formats passed issuance + VP.`);
  process.exit(failed ? 1 : 0);
}

main().catch((err) => {
  console.error('FATAL:', err);
  process.exit(1);
});
