import { Injectable, Logger } from '@nestjs/common';
import { IdentityClient } from '../clients/identity.client';
import * as jose from 'jose';

export interface PopResult {
  valid: boolean;
  holderDid?: string;
  holderJwk?: jose.JWK;
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
        const didDoc = await this.identity.resolveDID(holderDid);
        const vm = (didDoc.verificationMethod || []).find(
          (m: any) => m.id === header.kid && m.publicKeyJwk,
        );
        holderJwk = vm?.publicKeyJwk;
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

      return { valid: true, holderDid, holderJwk };
    } catch (err) {
      this.logger.warn(`PoP proof verification failed: ${err}`);
      return { valid: false, error: `${err}` };
    }
  }
}
