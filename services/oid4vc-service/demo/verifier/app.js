// Verifier Portal interactions. Cards are server-rendered from window.VERIFIERS;
// each "Request proof →" button starts a DCQL request for that use-case,
// renders the openid4vp QR, polls the result, and shows PASS/FAIL plus exactly
// what the wallet disclosed (should be only the requested attribute).
const OID4VC = window.OID4VC_BASE;
const VERIFIERS = window.VERIFIERS || [];

async function postJSON(url, body) {
  const r = await fetch(url, { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify(body) });
  if (!r.ok) throw new Error(`${url} -> ${r.status}: ${await r.text()}`);
  return r.json();
}
const getJSON = (url) => fetch(url).then((r) => r.json());

let polling = null;

async function start(v) {
  clearInterval(polling);
  const panel = document.getElementById('vp-panel');
  panel.innerHTML = `<div class="panel"><b>${v.name}</b><div class="row" style="margin-top:14px">
      <div id="qrbox" class="qr">requesting…</div><div id="vres"></div></div>
      <div id="vlog" class="muted"></div></div>`;
  try {
    const req = await postJSON(`${OID4VC}/vp/request`, { dcql_query: v.dcql });
    document.getElementById('qrbox').innerHTML =
      `<img src="/qr?data=${encodeURIComponent(req.qr_data)}" width="240" height="240"/>` +
      `<div class="muted" style="max-width:240px">${req.qr_data}</div>`;
    document.getElementById('vlog').textContent = 'Waiting for the wallet to present…';
    polling = setInterval(async () => {
      let s; try { s = await getJSON(`${OID4VC}/vp/status/${req.transaction_id}`); } catch { return; }
      if (s.status === 'pending') return;
      clearInterval(polling);
      render(v, s);
    }, 1500);
  } catch (e) {
    document.getElementById('qrbox').innerHTML = '';
    document.getElementById('vlog').textContent = 'Error: ' + e.message;
  }
}

function render(v, s) {
  const disclosed = (s.claims && (s.claims.age || Object.values(s.claims)[0])) || {};
  const ok = s.verified === true && disclosed[v.successKey] === true;
  const denied = s.verified === true && disclosed[v.successKey] !== true;
  const leaked = ['date_of_birth', 'full_name', 'national_id', 'gender'].filter((k) => k in disclosed);
  const checks = s.checks || {};
  const chips = Object.entries(checks).map(([k, val]) => `<span class="chk ${val === 'OK' ? '' : 'bad'}">${k}: ${val}</span>`).join('');
  document.getElementById('vres').innerHTML = `
    <div class="verdict ${ok ? 'pass' : 'fail'}">${ok ? '✅ ACCESS GRANTED — over 18' : (denied ? '⛔ ACCESS DENIED — not over 18' : '⛔ VERIFICATION FAILED')}</div>
    <p><b>Disclosed to verifier:</b></p>
    <pre>${JSON.stringify(disclosed, null, 2)}</pre>
    <p><b>Privacy:</b> ${leaked.length ? '⚠️ leaked ' + leaked.join(', ') : '✓ date of birth &amp; all other attributes stayed private'}</p>
    <div class="checks">${chips}</div>`;
  document.getElementById('vlog').textContent = '';
}

document.querySelectorAll('[data-verifier]').forEach((btn) => {
  btn.onclick = () => {
    const v = VERIFIERS.find((x) => x.id === btn.dataset.verifier);
    if (v) start(v);
  };
});
