import { Injectable, Logger } from '@nestjs/common';
import { IdentityClient } from '../clients/identity.client';
import { resolveSelfContainedDidToJwk } from '../utils/self-contained-did.util';
import * as jose from 'jose';

export interface PopResult {
  valid: boolean;
  holderDid?: string;
  holderJwk?: jose.JWK;
  // The proof header's `kid` verbatim (a DID URL including its fragment) when
  // the wallet bound via a DID rather than an inline JWK. SD-JWT VC must echo
  // this back as `cnf.kid`; see signSdJwtVc in credentials-service.
  holderKid?: string;
  error?: string;
}

// Verifies the wallet's proof-of-possession JWT (OID4VCI credential endpoint).
// This is the one genuinely security-critical new check: it proves the wallet
// controls the holder key, which we then bind into credentialSubject.id.
@Injectable()
export class PopService {
  private readonly logger = new Logger(PopService.name);

  constructor(private readonly identity: IdentityClient) {}

  // proof.jwt header: { typ: 'openid4vci-proof+jwt', alg, kid | jwk }
  // proof.jwt claims: { iss(=holder), aud(=issuer), iat, nonce(=c_nonce) }
  async verifyJwtProof(
    proofJwt: string,
    expected: { audience: string; nonce: string },
  ): Promise<PopResult> {
    try {
      const header = jose.decodeProtectedHeader(proofJwt);
      const claims: any = jose.decodeJwt(proofJwt);

      // Determine holder key: inline JWK, or resolve DID from kid.
      let holderJwk: jose.JWK | undefined = header.jwk as jose.JWK;
      let holderDid: string | undefined = claims.iss;
      if (!holderJwk && header.kid) {
        holderDid = header.kid.split('#')[0];
        // did:key/did:jwk encode the public key in the identifier itself, so
        // they resolve offline. identity-service's registry only knows its own
        // DB plus did:web and 500s on anything else — and Credo binds with
        // did:key whenever we advertise it in
        // `cryptographic_binding_methods_supported` (raw-JWK binding is not
        // permitted for jwt_vc_json), so without this every Credo-issued proof
        // fails as `invalid_proof: Error resolving DID`.
        holderJwk = resolveSelfContainedDidToJwk(holderDid);
        if (!holderJwk) {
          const didDoc = await this.identity.resolveDID(holderDid);
          const vm = (didDoc.verificationMethod || []).find(
            (m: any) => m.id === header.kid && m.publicKeyJwk,
          );
          holderJwk = vm?.publicKeyJwk;
        }
      }
      if (!holderJwk) {
        return { valid: false, error: 'No holder JWK found in proof (jwk/kid)' };
      }

      // Verify signature.
      const key = await jose.importJWK(holderJwk, (header.alg as string) || 'ES256');
      await jose.compactVerify(proofJwt, key);

      // Bind checks: audience + single-use nonce.
      if (claims.aud !== expected.audience) {
        return { valid: false, error: 'PoP audience mismatch' };
      }
      if (claims.nonce !== expected.nonce) {
        return { valid: false, error: 'PoP nonce mismatch' };
      }

      // Some wallets (e.g. did:jwk-based holders) sign an inline-jwk proof
      // with no `iss` claim — there's no DID to bind to, but the JWK itself
      // deterministically IS one (did:jwk spec: base64url of the JWK JSON).
      // Without this, credentialSubject/sub end up empty and downstream
      // storage fails on a missing required subjectId.
      if (!holderDid) {
        holderDid = `did:jwk:${Buffer.from(JSON.stringify(holderJwk)).toString('base64url')}`;
      }

      // Only a DID-bound proof carries a kid; an inline-jwk proof has none, and
      // must keep being bound by value (`cnf.jwk`).
      const holderKid = header.jwk ? undefined : (header.kid as string | undefined);

      return { valid: true, holderDid, holderJwk, holderKid };
    } catch (err) {
      this.logger.warn(`PoP proof verification failed: ${err}`);
      return { valid: false, error: `${err}` };
    }
  }
}
