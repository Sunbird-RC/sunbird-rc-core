// Headless proof of the Age Verification use case's core privacy property:
// issue a National Identity credential (5 attributes) then present ONLY
// `over_18`, and assert the verifier sees over_18 but NOT date_of_birth / name /
// national_id / gender.
import { postJSON, getJSON } from '../test-wallet/src/util.mjs';
import { createHolder } from '../test-wallet/src/wallet-core.mjs';
import { acceptOffer } from '../test-wallet/src/vci-client.mjs';
import { present } from '../test-wallet/src/vp-client.mjs';
import { CredentialStore } from '../test-wallet/src/store.mjs';

const OID4VC = process.env.OID4VC_BASE || 'http://localhost:3400';
const VCT = 'National Identity Credential';

async function configIdFor(vct, format) {
  const meta = await getJSON(`${OID4VC}/.well-known/openid-credential-issuer`, 'discovery');
  const supported = meta.credential_configurations_supported || meta.credentials_supported || {};
  const hit = Object.entries(supported).find(([, v]) => v.scope === vct && v.format === format);
  if (!hit) throw new Error(`config not found for ${vct}/${format} — run seed-national-id.mjs`);
  return hit[0];
}

async function run(label, over18) {
  console.log(`\n=== ${label} (over_18=${over18}) ===`);
  const holder = await createHolder();
  const store = new CredentialStore();
  const configId = await configIdFor(VCT, 'vc+sd-jwt');

  // Issue the full National Identity credential (all 5 attributes).
  const offer = await postJSON(`${OID4VC}/oid4vc/offer`, {
    credential_configuration_id: configId,
    format: 'vc+sd-jwt',
    claims: {
      national_id: 'NID-1234-5678',
      full_name: 'Kartheek Palla',
      date_of_birth: over18 ? '2000-05-20' : '2010-05-20',
      gender: 'M',
      over_18: over18,
    },
  }, {}, 'offer');
  const rec = await acceptOffer(offer.qr_data, holder);
  store.add(rec);
  console.log(`  issued; wallet holds all attributes: ${JSON.stringify(Object.keys(rec.claims || {}))}`);

  // Verifier asks for ONLY over_18.
  const req = await postJSON(`${OID4VC}/vp/request`, {
    dcql_query: {
      credentials: [
        { id: 'age', format: 'vc+sd-jwt', meta: { vct_values: [VCT] }, claims: [{ path: ['over_18'] }] },
      ],
    },
  }, {}, 'vp request');
  await present(req.qr_data, holder, store);
  const status = await getJSON(`${OID4VC}/vp/status/${req.transaction_id}`, 'vp status');

  const disclosed = status.claims?.age || {};
  const keys = Object.keys(disclosed);
  console.log(`  verified=${status.verified}  checks=${JSON.stringify(status.checks)}`);
  console.log(`  DISCLOSED to verifier: ${JSON.stringify(disclosed)}`);

  // Assertions
  const leaked = ['date_of_birth', 'full_name', 'national_id', 'gender'].filter((k) => k in disclosed);
  if (!status.verified) throw new Error('VP not verified');
  if (!('over_18' in disclosed)) throw new Error('over_18 not disclosed');
  if (leaked.length) throw new Error(`PRIVACY LEAK — disclosed private attrs: ${leaked.join(', ')}`);
  console.log(`  ✅ only over_18 disclosed (no DOB/name/id/gender leaked); over_18=${disclosed.over_18}`);
  return disclosed.over_18;
}

async function main() {
  const a = await run('Citizen ABOVE 18', true);
  const b = await run('Citizen BELOW 18', false);
  console.log('\n──────── SUMMARY ────────');
  console.log(`  over-18 citizen  -> disclosed over_18=${a}  (verifier: PASS)`);
  console.log(`  under-18 citizen -> disclosed over_18=${b}  (verifier: FAIL age check)`);
  console.log('\n✅ Selective disclosure proven: verifier learns only over_18, never DOB.');
}
main().catch((e) => { console.error('❌', e.message); process.exit(1); });
