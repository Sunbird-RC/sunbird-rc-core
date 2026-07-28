// Probe which formats actually issue for the Farmer Credential (rcw issuer DID).
import { createHolder } from '../test-wallet/src/wallet-core.mjs';
import { acceptOffer } from '../test-wallet/src/vci-client.mjs';

const OID4VC = process.env.OID4VC_BASE || 'http://localhost:3400';
const NAME = process.env.CRED || 'Farmer Credential';
const CLAIMS = {
  farmer_id: 'FRM-1001', full_name: 'Ravi Kumar', gender: 'M',
  land_area_acres: 4.5, ownership_type: 'Owned', land_record_ref: 'LR-77-2201',
  primary_crop: 'Wheat', farm_location: 'Village Rampur',
};

async function jget(u) { return (await fetch(u, { headers: { accept: 'application/json' } })).json(); }
async function jpost(u, b) {
  const r = await fetch(u, { method: 'POST', headers: { 'content-type': 'application/json', accept: 'application/json' }, body: JSON.stringify(b) });
  if (!r.ok) throw new Error(`${r.status}: ${(await r.text()).slice(0, 160)}`);
  return r.json();
}

async function main() {
  const meta = await jget(`${OID4VC}/.well-known/openid-credential-issuer`);
  const supported = meta.credential_configurations_supported || {};
  const mine = Object.entries(supported).filter(([, v]) => v.scope === NAME);
  console.log(`Configs for "${NAME}":`, mine.map(([id, v]) => `${v.format}`).join(', '));

  for (const [configId, v] of mine) {
    const holder = await createHolder();
    try {
      const offer = await jpost(`${OID4VC}/oid4vc/offer`, { credential_configuration_id: configId, format: v.format, claims: CLAIMS });
      const rec = await acceptOffer(offer.qr_data, holder);
      console.log(`  ✅ ${v.format.padEnd(12)} issued (${rec.raw ? 'ok' : '?'})`);
    } catch (e) {
      console.log(`  ❌ ${v.format.padEnd(12)} ${e.message}`);
    }
  }
}
main().catch((e) => { console.error(e); process.exit(1); });
