// Unit test for the wallet-side trusted-verifier allowlist predicate
// (test-wallet/src/trust.mjs). Pure logic, no network — run directly:
//   node test/trust.spec.mjs
import assert from 'node:assert/strict';
import {
  setTrustedVerifiers,
  resetTrustedVerifiers,
  isTrustedVerifier,
} from '../src/trust.mjs';

function reqObj(overrides = {}) {
  return { client_id: 'did:web:verifier.example', response_uri: 'https://verifier.example/vp/response', ...overrides };
}

// 1. Not configured (null) => allow-all-with-a-warning.
resetTrustedVerifiers();
assert.equal(isTrustedVerifier(reqObj(), { verified: true }), true, 'unconfigured allowlist should allow-all');

// 2. Trusted, verified, matching response_uri origin => allowed.
setTrustedVerifiers([{ clientId: 'did:web:verifier.example', responseUriOrigin: 'https://verifier.example' }]);
assert.equal(isTrustedVerifier(reqObj(), { verified: true, signed: true }), true, 'trusted + verified should be allowed');

// 3. Untrusted client_id => refused.
assert.equal(
  isTrustedVerifier(reqObj({ client_id: 'did:web:attacker.example' }), { verified: true, signed: true }),
  false,
  'unknown client_id must be refused',
);

// 4. response_uri origin mismatch (signature-valid request re-pointed at a
//    different endpoint) => refused, even though client_id matches.
assert.equal(
  isTrustedVerifier(reqObj({ response_uri: 'https://attacker.example/collect' }), { verified: true, signed: true }),
  false,
  'response_uri origin mismatch must be refused',
);

// 5. Unsigned/unverified request against an entry that does NOT opt in via
//    allowUnsigned => refused.
assert.equal(
  isTrustedVerifier(reqObj(), { verified: false, signed: false }),
  false,
  'unsigned request must be refused unless the entry opts in',
);

// 6. Unsigned/unverified request against an entry that DOES opt in
//    (the legacy/walt.id demo path) => allowed.
setTrustedVerifiers([
  { clientId: 'did:web:verifier.example', responseUriOrigin: 'https://verifier.example', allowUnsigned: true },
]);
assert.equal(
  isTrustedVerifier(reqObj(), { verified: false, signed: false }),
  true,
  'unsigned request must be allowed when the entry sets allowUnsigned',
);

// 7. Missing client_id/response_uri on the request object => refused.
setTrustedVerifiers([{ clientId: 'did:web:verifier.example', responseUriOrigin: 'https://verifier.example' }]);
assert.equal(isTrustedVerifier({}, { verified: true }), false, 'missing client_id/response_uri must be refused');

console.log('trust.spec.mjs: all assertions passed');
