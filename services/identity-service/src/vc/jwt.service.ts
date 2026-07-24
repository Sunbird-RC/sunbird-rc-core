import {
  Injectable,
  InternalServerErrorException,
  Logger,
  NotFoundException,
  BadRequestException,
} from '@nestjs/common';
import { PrismaService } from '../utils/prisma.service';
import { VaultService } from '../utils/vault.service';
import { DidService } from '../did/did.service';
import { Identity } from '@prisma/client';
import * as crypto from 'crypto';
import * as jose from 'jose';

const ES256 = 'ES256';
const SD_ALG = 'sha-256';

export interface SdJwtVerifyResult {
  verified: boolean;
  claims?: Record<string, any>;
  error?: string;
}

// JWS/JWT + SD-JWT signing for OID4VC formats (jwt_vc_json, vc+sd-jwt).
// Additive to the existing JSON-LD signing path in VcService:
// - P-256 (ES256) keys are appended as extra verificationMethods on the DID
//   document (index >= 1), so VcService.sign() — which uses index 0 — is untouched.
// - Private JWKs live in Vault under the same secret as the Ed25519 keys,
//   keyed by verificationMethod id (same convention as VcService.getSuite()).
@Injectable()
export class JwtSignerService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly vault: VaultService,
    private readonly didService: DidService,
  ) {}

  // Finds the DID's ES256 verification method, creating one if absent.
  async ensureES256Key(
    signerDID: string,
  ): Promise<{ kid: string; privateJwk: jose.JWK }> {
    let identity: Identity;
    try {
      identity = await this.prisma.identity.findUnique({
        where: { id: signerDID },
      });
    } catch (err) {
      Logger.error('Error fetching signerDID:', err);
      throw new InternalServerErrorException('Error fetching signerDID');
    }
    if (!identity) throw new NotFoundException('Signer DID not found!');

    const didDoc = this.parseDidDoc(identity);
    const existing = (didDoc.verificationMethod || []).find(
      (vm: any) => vm?.publicKeyJwk?.crv === 'P-256',
    );
    if (existing) {
      const privateKeys = (await this.vault.readPvtKey(signerDID)) || {};
      const privateJwk = privateKeys[existing.id]?.privateKeyJwk;
      if (!privateJwk) {
        throw new InternalServerErrorException(
          'ES256 private key missing from vault for ' + existing.id,
        );
      }
      return { kid: existing.id, privateJwk };
    }
    return this.generateES256Key(signerDID, didDoc);
  }

  private async generateES256Key(signerDID: string, didDoc: any) {
    const kid = `${signerDID}#jwt-key-1`;
    let publicJwk: jose.JWK;
    let privateJwk: jose.JWK;
    try {
      const { publicKey, privateKey } = await jose.generateKeyPair(ES256, {
        extractable: true,
      });
      publicJwk = await jose.exportJWK(publicKey);
      privateJwk = await jose.exportJWK(privateKey);
      publicJwk.alg = ES256;
      publicJwk.use = 'sig';
      publicJwk.kid = kid;
    } catch (err) {
      Logger.error(`Error generating ES256 key pair: ${err}`);
      throw new InternalServerErrorException('Error generating ES256 key pair');
    }

    const verificationMethod = {
      id: kid,
      type: 'JsonWebKey2020',
      controller: signerDID,
      publicKeyJwk: publicJwk,
    };
    didDoc.verificationMethod = [
      ...(didDoc.verificationMethod || []),
      verificationMethod,
    ];
    didDoc.assertionMethod = [...(didDoc.assertionMethod || []), kid];

    try {
      await this.prisma.identity.update({
        where: { id: signerDID },
        data: { didDoc: JSON.stringify(didDoc) },
      });
    } catch (err) {
      Logger.error(`Error updating DID document with ES256 key: ${err}`);
      throw new InternalServerErrorException('Error updating DID document');
    }
    try {
      await this.vault.mergePvtKey({ [kid]: { privateKeyJwk: privateJwk } }, signerDID);
    } catch (err) {
      Logger.error(`Error writing ES256 key to vault: ${err}`);
      throw new InternalServerErrorException('Error writing ES256 key to vault');
    }
    return { kid, privateJwk };
  }

  // Signs an arbitrary JSON payload as a compact JWS (ES256).
  // The caller controls the claim set; only alg/kid/typ go in the header.
  async signJwt(
    signerDID: string,
    payload: object,
    header: Record<string, any> = {},
  ): Promise<string> {
    const { kid, privateJwk } = await this.ensureES256Key(signerDID);
    try {
      const key = await jose.importJWK(privateJwk, ES256);
      return await new jose.CompactSign(
        new TextEncoder().encode(JSON.stringify(payload)),
      )
        .setProtectedHeader({ ...header, alg: ES256, kid })
        .sign(key);
    } catch (err) {
      Logger.error('Error signing JWT:', err);
      throw new InternalServerErrorException('Error signing JWT');
    }
  }

  // Verifies a compact JWS against the signer's DID document (kid header,
  // or an explicitly provided DID). Returns the decoded payload on success.
  async verifyJwt(
    token: string,
    signerDID?: string,
  ): Promise<{ verified: boolean; payload?: any; error?: string }> {
    try {
      const header = jose.decodeProtectedHeader(token);
      const kid = header.kid;
      const did = signerDID || (kid ? kid.split('#')[0] : undefined);
      if (!did) return { verified: false, error: 'No kid header or DID given' };
      const didDoc = await this.didService.resolveDID(did);
      const vms = (didDoc.verificationMethod || []).filter(
        (vm: any) => vm?.publicKeyJwk,
      );
      const vm = kid ? vms.find((m: any) => m.id === kid) || vms[0] : vms[0];
      if (!vm) return { verified: false, error: 'No JWK verification method on DID' };
      const key = await jose.importJWK(vm.publicKeyJwk, (header.alg as string) || ES256);
      const { payload } = await jose.compactVerify(token, key);
      return { verified: true, payload: JSON.parse(new TextDecoder().decode(payload)) };
    } catch (err) {
      return { verified: false, error: `${err}` };
    }
  }

  // --- SD-JWT (vc+sd-jwt) --------------------------------------------------

  // Issues an SD-JWT: claims listed in `disclosable` are replaced by salted
  // SHA-256 digests in `_sd`; each disclosure travels alongside the JWT.
  // Output: <issuer-jws>~<disclosure-1>~...~<disclosure-n>~
  async signSdJwt(
    signerDID: string,
    payload: Record<string, any>,
    disclosable: string[] = [],
    header: Record<string, any> = {},
  ): Promise<string> {
    const sdPayload: Record<string, any> = { ...payload };
    const disclosures: string[] = [];
    const digests: string[] = [];

    for (const claim of disclosable) {
      if (!(claim in sdPayload)) continue;
      const salt = jose.base64url.encode(crypto.randomBytes(16));
      const disclosure = jose.base64url.encode(
        JSON.stringify([salt, claim, sdPayload[claim]]),
      );
      disclosures.push(disclosure);
      digests.push(this.disclosureDigest(disclosure));
      delete sdPayload[claim];
    }

    if (digests.length) {
      sdPayload._sd = digests.sort();
      sdPayload._sd_alg = SD_ALG;
    }

    const jws = await this.signJwt(signerDID, sdPayload, {
      typ: 'vc+sd-jwt',
      ...header,
    });
    return [jws, ...disclosures, ''].join('~');
  }

  // Verifies an SD-JWT (optionally with key-binding JWT) and reconstructs the
  // disclosed claims. KB-JWT nonce/audience checks are the caller's contract.
  async verifySdJwt(
    sdJwt: string,
    signerDID?: string,
    keyBinding?: { nonce?: string; audience?: string },
  ): Promise<SdJwtVerifyResult> {
    try {
      const endsWithTilde = sdJwt.endsWith('~');
      const parts = sdJwt.split('~');
      const jws = parts[0];
      // If the string does NOT end with '~', the final segment is a KB-JWT.
      const kbJwt = endsWithTilde ? null : parts.pop();
      const disclosures = parts.slice(1).filter((d) => d.length > 0);

      const jwsResult = await this.verifyJwt(jws, signerDID);
      if (!jwsResult.verified) {
        return { verified: false, error: `Issuer signature invalid: ${jwsResult.error}` };
      }
      const payload = jwsResult.payload;
      const sdDigests: string[] = payload._sd || [];

      const claims: Record<string, any> = { ...payload };
      delete claims._sd;
      delete claims._sd_alg;

      for (const disclosure of disclosures) {
        const digest = this.disclosureDigest(disclosure);
        if (!sdDigests.includes(digest)) {
          return { verified: false, error: 'Disclosure digest not found in _sd' };
        }
        const [, name, value] = JSON.parse(
          new TextDecoder().decode(jose.base64url.decode(disclosure)),
        );
        claims[name] = value;
      }

      if (kbJwt) {
        const cnfJwk = payload?.cnf?.jwk;
        if (!cnfJwk) return { verified: false, error: 'KB-JWT present but no cnf.jwk in SD-JWT' };
        try {
          const kbHeader = jose.decodeProtectedHeader(kbJwt);
          const key = await jose.importJWK(cnfJwk, (kbHeader.alg as string) || ES256);
          const { payload: kbRaw } = await jose.compactVerify(kbJwt, key);
          const kbPayload = JSON.parse(new TextDecoder().decode(kbRaw));
          if (keyBinding?.nonce && kbPayload.nonce !== keyBinding.nonce) {
            return { verified: false, error: 'KB-JWT nonce mismatch' };
          }
          if (keyBinding?.audience && kbPayload.aud !== keyBinding.audience) {
            return { verified: false, error: 'KB-JWT audience mismatch' };
          }
        } catch (err) {
          return { verified: false, error: `KB-JWT invalid: ${err}` };
        }
      }

      return { verified: true, claims };
    } catch (err) {
      return { verified: false, error: `${err}` };
    }
  }

  private disclosureDigest(disclosure: string): string {
    return jose.base64url.encode(
      crypto.createHash('sha256').update(disclosure, 'ascii').digest(),
    );
  }

  // --- JWKS ----------------------------------------------------------------

  // Public JWKs of every DID that carries a publicKeyJwk verification method.
  // Only issuer DIDs gain ES256 keys (via signJwt), so this set stays small.
  async getJwks(): Promise<{ keys: jose.JWK[] }> {
    let identities: Identity[] = [];
    try {
      identities = await this.prisma.identity.findMany({
        where: { didDoc: { string_contains: 'publicKeyJwk' } as any },
      });
    } catch (err) {
      // string_contains unsupported on this connector — fall back to full scan
      Logger.warn(`JWKS json filter failed (${err}); falling back to full scan`);
      identities = await this.prisma.identity.findMany();
    }
    const keys: jose.JWK[] = [];
    for (const identity of identities) {
      let didDoc: any;
      try {
        didDoc = this.parseDidDoc(identity);
      } catch {
        continue;
      }
      for (const vm of didDoc?.verificationMethod || []) {
        if (vm?.publicKeyJwk) {
          keys.push({ ...vm.publicKeyJwk, kid: vm.publicKeyJwk.kid || vm.id });
        }
      }
    }
    return { keys };
  }

  private parseDidDoc(identity: Identity): any {
    const raw = identity.didDoc;
    if (typeof raw === 'string') return JSON.parse(raw);
    if (raw && typeof raw === 'object') return raw;
    throw new BadRequestException('Malformed DID document');
  }
}
