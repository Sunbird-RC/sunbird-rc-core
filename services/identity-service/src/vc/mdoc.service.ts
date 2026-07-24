import { Injectable, Logger, NotFoundException, InternalServerErrorException } from '@nestjs/common';
import { PrismaService } from '../utils/prisma.service';
import { VaultService } from '../utils/vault.service';
import { Identity } from '@prisma/client';
import * as nodeCrypto from 'crypto';
// @types/node is pinned to 16.0.0 in this service (predates the `webcrypto`
// named export) even though the runtime is Node 20 and has it — cast rather
// than bump a repo-wide type dependency for one file.
const webcrypto: any = (nodeCrypto as any).webcrypto;
import { Document, parse } from '@auth0/mdl';
import { Verifier } from '@auth0/mdl';
import * as x509 from '@peculiar/x509';

// mso_mdoc (ISO/IEC 18013-5) issuance and verification. Kept separate from
// jwt.service.ts/vc.service.ts since mdoc is CBOR/COSE, not JOSE/JSON-LD —
// a genuinely different wire format, not just another proof suite.
@Injectable()
export class MdocService {
  private readonly logger = new Logger(MdocService.name);

  constructor(
    private readonly prisma: PrismaService,
    private readonly vault: VaultService,
  ) {}

  // Resolves the signer DID's EC P-256 (JsonWebKey2020) key material — the
  // same Vault-backed lookup pattern vc.service.ts's sign() already uses for
  // Ed25519, just for the JWK verification method type mdoc requires.
  private async getSignerKeyMaterial(signerDID: string) {
    let did: Identity;
    try {
      did = await this.prisma.identity.findUnique({ where: { id: signerDID } });
    } catch (err) {
      this.logger.error('Error fetching signerDID:', err);
      throw new InternalServerErrorException('Error fetching signerDID');
    }
    if (!did) throw new NotFoundException('Signer DID not found!');

    const didDoc = JSON.parse(did.didDoc as string);
    const vm = (didDoc.verificationMethod || []).find((v: any) => v.type === 'JsonWebKey2020');
    if (!vm) {
      throw new NotFoundException(
        `DID '${signerDID}' has no JsonWebKey2020 (EC P-256) verification method — required for mso_mdoc issuance`,
      );
    }
    const privateKeys = (await this.vault.readPvtKey(signerDID)) || {};
    const privateKeyJwk = privateKeys[vm.id]?.privateKeyJwk;
    if (!privateKeyJwk) {
      throw new InternalServerErrorException(`Private key for '${vm.id}' not found in Vault`);
    }
    return { publicKeyJwk: vm.publicKeyJwk, privateKeyJwk };
  }

  // Generates a fresh self-signed X.509 cert wrapping the issuer's own
  // public key. This deployment has no real IACA root chain (same trust
  // model as every other format here: we trust our own DIDs' key material
  // directly, not a PKI) — verification correspondingly always passes
  // `disableCertificateChainValidation: true`, so the cert only needs to
  // correctly wrap the actual signing key, not chain to anything.
  private async buildSelfSignedCert(
    signerDID: string,
    publicKeyJwk: any,
    privateKeyJwk: any,
  ): Promise<string> {
    const publicKey = await webcrypto.subtle.importKey(
      'jwk',
      publicKeyJwk,
      { name: 'ECDSA', namedCurve: 'P-256' },
      true,
      ['verify'],
    );
    const privateKey = await webcrypto.subtle.importKey(
      'jwk',
      privateKeyJwk,
      { name: 'ECDSA', namedCurve: 'P-256' },
      true,
      ['sign'],
    );
    const cert = await x509.X509CertificateGenerator.createSelfSigned({
      serialNumber: Date.now().toString(16),
      // The standard org.iso.18013.5.1 (mDL) namespace requires the
      // issuer cert's subject to carry a Country (C) — found live: without
      // it, @auth0/mdl's own mDL-specific check
      // ("Country name (C) must be present...") fails even though the
      // signature itself verifies fine. Harmless placeholder for non-mDL
      // custom namespaces too.
      name: `CN=${signerDID}, C=US`,
      notBefore: new Date(),
      notAfter: new Date(Date.now() + 365 * 24 * 60 * 60 * 1000),
      signingAlgorithm: { name: 'ECDSA', hash: 'SHA-256' },
      keys: { publicKey, privateKey },
    });
    return cert.toString('pem');
  }

  // Signs an mso_mdoc credential. Returns the base64url-encoded CBOR MDoc
  // (analogous to the compact-JWT/SD-JWT string envelope the other
  // enveloped formats use).
  async signMdoc(
    signerDID: string,
    docType: string,
    namespaces: Record<string, Record<string, any>>,
    deviceKeyJwk?: Record<string, any>,
  ): Promise<string> {
    const { publicKeyJwk, privateKeyJwk } = await this.getSignerKeyMaterial(signerDID);
    const issuerCertificate = await this.buildSelfSignedCert(
      signerDID,
      publicKeyJwk as any,
      privateKeyJwk as any,
    );

    let document = new Document(docType);
    for (const [namespace, values] of Object.entries(namespaces)) {
      document = document.addIssuerNameSpace(namespace, values);
    }
    document = document.useDigestAlgorithm('SHA-256').addValidityInfo({});
    if (deviceKeyJwk) {
      document = document.addDeviceKeyInfo({ deviceKey: deviceKeyJwk as any });
    }

    try {
      const signed = await document.sign({
        issuerPrivateKey: privateKeyJwk as any,
        issuerCertificate,
        alg: 'ES256',
      });
      const { MDoc } = await import('@auth0/mdl');
      const mdoc = new MDoc([signed]);
      return mdoc.encode().toString('base64url');
    } catch (err) {
      this.logger.error('Error signing mdoc:', err);
      throw new InternalServerErrorException('Error signing mdoc');
    }
  }

  // Verifies a standalone mso_mdoc credential (issuer signature + per-item
  // digests) — no device/presentation context. Deliberately reuses
  // @auth0/mdl's own Verifier internals (verifyIssuerSignature/verifyData)
  // rather than reimplementing COSE_Sign1/digest verification by hand; those
  // methods are TS-`private`-annotated only (not real JS `#private`), so
  // they're runtime-accessible — the same library that built these digests
  // at signing time is the most reliable thing to check them, versus a
  // hand-rolled reimplementation of ISO 18013-5 Annex B digest matching.
  async verifyMdoc(encodedMdoc: string): Promise<{ verified: boolean; claims?: Record<string, any>; docType?: string; error?: string }> {
    try {
      const bytes = Buffer.from(encodedMdoc, 'base64url');
      const mdoc = parse(bytes);
      const doc = mdoc.documents[0];
      if (!doc) return { verified: false, error: 'No documents in mdoc' };

      const failures: any[] = [];
      const onCheck = (result: any) => {
        if (result.status === 'FAILED') failures.push(result);
      };
      const verifier: any = new Verifier([]);
      await verifier.verifyIssuerSignature(doc.issuerSigned.issuerAuth, true, onCheck);
      await verifier.verifyData(doc, onCheck);

      if (failures.length) {
        return { verified: false, error: failures.map((f) => f.check).join('; ') };
      }
      const claims: Record<string, any> = {};
      for (const ns of doc.issuerSignedNameSpaces) {
        claims[ns] = doc.getIssuerNameSpace(ns);
      }
      return { verified: true, claims, docType: doc.docType };
    } catch (err) {
      this.logger.error('Error verifying mdoc:', err);
      return { verified: false, error: `${err}` };
    }
  }
}
