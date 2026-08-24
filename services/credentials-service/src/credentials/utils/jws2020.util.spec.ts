import * as crypto from 'crypto';
import * as jsonld from 'jsonld';
import { verifyJsonWebSignature2020 } from './jws2020.util';

// Fully offline (no network context fetch): a tiny inline @context covering
// only the terms this test's documents actually use, so canonicalization
// never needs to fetch anything.
const CONTEXT = {
  '@context': {
    '@vocab': 'https://example.test/vocab#',
    id: '@id',
    type: '@type',
    JsonWebSignature2020: 'https://w3id.org/security#JsonWebSignature2020',
    proof: { '@id': 'https://w3id.org/security#proof', '@type': '@id', '@container': '@graph' },
    challenge: 'https://w3id.org/security#challenge',
    domain: 'https://w3id.org/security#domain',
    proofPurpose: { '@id': 'https://w3id.org/security#proofPurpose', '@type': '@vocab' },
    verificationMethod: { '@id': 'https://w3id.org/security#verificationMethod', '@type': '@id' },
    created: { '@id': 'http://purl.org/dc/terms/created', '@type': 'http://www.w3.org/2001/XMLSchema#dateTime' },
  },
};
const CONTEXT_URL = 'https://example.test/context';

async function documentLoader(url: string) {
  if (url === CONTEXT_URL) return { contextUrl: null, documentUrl: url, document: CONTEXT };
  throw new Error(`unexpected context fetch: ${url}`);
}

// Test-only signer implementing the exact same "Create Verify Hash"
// algorithm the util verifies against — this is a self-consistency check
// (proves sign/verify agree and tampering is rejected), NOT proof that a
// real wallet's actual JsonWebSignature2020 output verifies; that needs a
// live test against the real wallet.
async function signJws2020(document: any, privateKey: crypto.KeyObject, alg: string): Promise<string> {
  const header = { alg, b64: false, crit: ['b64'] };
  const headerB64 = Buffer.from(JSON.stringify(header)).toString('base64url');

  const { jws: _jws, ...proofOptions } = document.proof;
  if (!proofOptions['@context']) proofOptions['@context'] = document['@context'];
  const { proof: _proof, ...docWithoutProof } = document;

  const [optionsCanon, docCanon] = await Promise.all([
    jsonld.canonize(proofOptions, { algorithm: 'URDNA2015', documentLoader } as any),
    jsonld.canonize(docWithoutProof, { algorithm: 'URDNA2015', documentLoader } as any),
  ]);
  const optionsHash = crypto.createHash('sha256').update(optionsCanon, 'utf8').digest();
  const docHash = crypto.createHash('sha256').update(docCanon, 'utf8').digest();
  const verifyData = Buffer.concat([optionsHash, docHash]);
  const signingInput = Buffer.concat([Buffer.from(headerB64, 'ascii'), Buffer.from('.'), verifyData]);

  let signature: Buffer;
  if (alg === 'EdDSA') {
    signature = crypto.sign(null, signingInput, privateKey);
  } else {
    signature = crypto.sign('sha256', signingInput, { key: privateKey, dsaEncoding: 'ieee-p1363' });
  }
  return `${headerB64}..${signature.toString('base64url')}`;
}

function buildVp(proofExtra: Record<string, any>): any {
  return {
    '@context': CONTEXT_URL,
    type: 'VerifiablePresentation',
    id: 'urn:uuid:test',
    holder: 'did:jwk:test',
    proof: {
      type: 'JsonWebSignature2020',
      created: '2026-01-01T00:00:00Z',
      proofPurpose: 'authentication',
      verificationMethod: 'did:jwk:test#0',
      jws: undefined as string | undefined,
      ...proofExtra,
    },
  };
}

describe('verifyJsonWebSignature2020', () => {
  it('verifies a correctly-signed ES256 (P-256) proof', async () => {
    const { publicKey, privateKey } = crypto.generateKeyPairSync('ec', { namedCurve: 'P-256' });
    const publicJwk = publicKey.export({ format: 'jwk' }) as any;

    const vp = buildVp({ challenge: 'n1', domain: 'https://verifier.example/vp/response' });
    vp.proof.jws = await signJws2020(vp, privateKey, 'ES256');

    const res = await verifyJsonWebSignature2020(vp, publicJwk, 'authentication', documentLoader);
    expect(res).toEqual({ verified: true });
  });

  it('verifies a correctly-signed EdDSA (Ed25519) proof', async () => {
    const { publicKey, privateKey } = crypto.generateKeyPairSync('ed25519');
    const publicJwk = publicKey.export({ format: 'jwk' }) as any;

    const vp = buildVp({ challenge: 'n1', domain: 'https://verifier.example/vp/response' });
    vp.proof.jws = await signJws2020(vp, privateKey, 'EdDSA');

    const res = await verifyJsonWebSignature2020(vp, publicJwk, 'authentication', documentLoader);
    expect(res).toEqual({ verified: true });
  });

  it('rejects a tampered document (signature no longer matches)', async () => {
    const { publicKey, privateKey } = crypto.generateKeyPairSync('ec', { namedCurve: 'P-256' });
    const publicJwk = publicKey.export({ format: 'jwk' }) as any;

    const vp = buildVp({ challenge: 'n1', domain: 'https://verifier.example/vp/response' });
    vp.proof.jws = await signJws2020(vp, privateKey, 'ES256');
    (vp as any).holder = 'did:jwk:attacker-substituted';

    const res = await verifyJsonWebSignature2020(vp, publicJwk, 'authentication', documentLoader);
    expect(res.verified).toBe(false);
  });

  it('rejects when proofPurpose does not match what the caller expects', async () => {
    const { publicKey, privateKey } = crypto.generateKeyPairSync('ec', { namedCurve: 'P-256' });
    const publicJwk = publicKey.export({ format: 'jwk' }) as any;

    const vp = buildVp({ challenge: 'n1', domain: 'https://verifier.example/vp/response' });
    vp.proof.jws = await signJws2020(vp, privateKey, 'ES256');

    const res = await verifyJsonWebSignature2020(vp, publicJwk, 'assertionMethod', documentLoader);
    expect(res).toEqual({
      verified: false,
      error: "expected proofPurpose 'assertionMethod', got 'authentication'",
    });
  });

  it('rejects a non-detached (embedded payload) JWS', async () => {
    const vp = buildVp({ challenge: 'n1', domain: 'https://verifier.example/vp/response' });
    vp.proof.jws = 'header.payload.signature';
    const { publicKey } = crypto.generateKeyPairSync('ec', { namedCurve: 'P-256' });
    const res = await verifyJsonWebSignature2020(
      vp,
      publicKey.export({ format: 'jwk' }) as any,
      'authentication',
      documentLoader,
    );
    expect(res.verified).toBe(false);
    expect(res.error).toMatch(/detached JWS/);
  });

  it('rejects a signature from the wrong key', async () => {
    const { privateKey } = crypto.generateKeyPairSync('ec', { namedCurve: 'P-256' });
    const { publicKey: wrongPublicKey } = crypto.generateKeyPairSync('ec', { namedCurve: 'P-256' });

    const vp = buildVp({ challenge: 'n1', domain: 'https://verifier.example/vp/response' });
    vp.proof.jws = await signJws2020(vp, privateKey, 'ES256');

    const res = await verifyJsonWebSignature2020(
      vp,
      wrongPublicKey.export({ format: 'jwk' }) as any,
      'authentication',
      documentLoader,
    );
    expect(res.verified).toBe(false);
  });
});