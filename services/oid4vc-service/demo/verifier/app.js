// Verifier Portal interactions. Cards are server-rendered from window.VERIFIERS;
// each "Request proof →" builds a DCQL query for the selected format, shows the
// openid4vp QR, polls the result, and shows what the wallet disclosed.
const OID4VC = window.OID4VC_BASE;
const VERIFIERS = window.VERIFIERS || [];

async function postJSON(url, body) {
  const r = await fetch(url, { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify(body) });
  if (!r.ok) throw new Error(`${url} -> ${r.status}: ${await r.text()}`);
  return r.json();
}
const getJSON = (url) => fetch(url).then((r) => r.json());
let polling = null;

// Build a format-specific DCQL credential query.
function buildDcql(v, format) {
  const req = v.request;
  if (format === 'mso_mdoc') {
    return { credentials: [{ id: 'q', format, meta: { doctype_value: v.docType }, claims: req.map((c) => ({ path: [v.namespace, c] })) }] };
  }
  if (format === 'vc+sd-jwt') {
    // vct_values must match the `vct` claim actually embedded in the
    // credential — which is the normalized URI oid4vc-service publishes (see
    // vct.util.ts normalizeVct()), not the bare schema name. `v.sdJwtVctValues`
    // is that resolved value, computed server-side in server.mjs against
    // live issuer metadata (found live: with the bare name here, walt.id held
    // a credential whose real vct was the URI and reported "no credentials
    // matching this presentation request").
    // DCQL wire format is `dc+sd-jwt`, not `vc+sd-jwt` — the SD-JWT VC format
    // id was renamed partway through the spec's drafts, and real wallets
    // (walt.id) enforce it strictly: a DCQL query with `format: 'vc+sd-jwt'`
    // 400s with "CredentialFormat does not contain element with name
    // 'vc+sd-jwt'" before the request is even parsed. `format` here still
    // means the OID4VCI credential format (used for the UI/issuance side);
    // only the value sent in the query needs the DCQL spelling.
    return { credentials: [{ id: 'q', format: 'dc+sd-jwt', meta: { vct_values: v.sdJwtVctValues || v.typeNames }, claims: req.map((c) => ({ path: [c] })) }] };
  }
  // ldp_vc / jwt_vc_json — match by type, claims under credentialSubject.
  return { credentials: [{ id: 'q', format, meta: { type_values: v.typeNames.map((t) => ['VerifiableCredential', t]) }, claims: req.map((c) => ({ path: ['credentialSubject', c] })) }] };
}

async function start(v, format, out) {
  clearInterval(polling);
  out.className = 'qr-panel'; // was .empty
  out.innerHTML = `<div class="pill" style="margin-bottom:10px">${format}</div>
    <div id="qrbox" class="qr">requesting…</div><div id="vres"></div><div id="vlog" class="muted"></div>`;
  const $ = (id) => out.querySelector('#' + id);
  try {
    const req = await postJSON(`${OID4VC}/vp/request`, { dcql_query: buildDcql(v, format) });
    $('qrbox').innerHTML =
      `<img src="/qr?data=${encodeURIComponent(req.qr_data)}" width="220" height="220"/>` +
      `<div class="muted" style="word-break:break-all">${req.qr_data}</div>`;
    $('vlog').textContent = 'Waiting for the wallet to present…';
    polling = setInterval(async () => {
      let s; try { s = await getJSON(`${OID4VC}/vp/status/${req.transaction_id}`); } catch { return; }
      if (s.status === 'pending') return;
      clearInterval(polling);
      render(v, format, s, out);
    }, 1500);
  } catch (e) {
    $('qrbox').innerHTML = '';
    $('vlog').textContent = 'Error: ' + e.message;
  }
}

function render(v, format, s, out) {
  const disclosed = (s.claims && (s.claims.q || Object.values(s.claims)[0])) || {};
  const sd = format === 'vc+sd-jwt' || format === 'mso_mdoc';
  // mdoc claims come back either nested under the namespace or as dotted
  // "<namespace>.<element>" keys — normalise to clean element names.
  let flat = disclosed;
  if (format === 'mso_mdoc') {
    if (disclosed[v.namespace] && typeof disclosed[v.namespace] === 'object') flat = disclosed[v.namespace];
    else { flat = {}; for (const [k, val] of Object.entries(disclosed)) flat[k.startsWith(v.namespace + '.') ? k.slice(v.namespace.length + 1) : k] = val; }
  }
  const verified = s.verified === true;
  const ok = verified && (v.successKey ? flat[v.successKey] === true : true);
  const shown = Object.keys(flat);
  const extra = sd ? shown.filter((k) => !v.request.includes(k) && !['iss', 'iat', 'vct', 'cnf', 'jti', 'sub'].includes(k)) : [];
  const checks = s.checks || {};
  const chips = Object.entries(checks).map(([k, val]) => `<span class="chk ${val === 'OK' ? '' : 'bad'}">${k}: ${val}</span>`).join('');

  out.querySelector('#vres').innerHTML = `
    <div class="verdict ${ok ? 'pass' : 'fail'}">${ok ? '✅ VERIFIED' : (verified ? '⛔ NOT ELIGIBLE' : '⛔ VERIFICATION FAILED')}</div>
    <p style="text-align:left"><b>Disclosed to verifier:</b></p>
    <pre style="text-align:left">${JSON.stringify(flat, null, 2)}</pre>
    <p style="text-align:left"><b>Privacy:</b> ${sd
      ? (extra.length ? '⚠️ extra fields present: ' + extra.join(', ') : '✓ only the requested attributes were shared')
      : '⚠️ ' + format + ' shares the whole credential (no selective disclosure)'}</p>
    <div class="checks">${chips}</div>`;
  out.querySelector('#vlog').textContent = '';
}

document.querySelectorAll('[data-verifier]').forEach((btn) => {
  btn.onclick = () => {
    const out = btn.closest('.tab-panel').querySelector('.vp-out');
    const format = btn.closest('.tab-panel').querySelector('.fmt').value;
    const v = VERIFIERS.find((x) => x.id === btn.dataset.verifier);
    if (v) start(v, format, out);
  };
});
