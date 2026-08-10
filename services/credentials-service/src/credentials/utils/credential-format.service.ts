import { Injectable, InternalServerErrorException, Logger } from '@nestjs/common';
import { IdentityUtilsService } from './identity.utils.service';
import { CredentialPayload, IssuerType, W3CCredential } from 'vc.types';
import { CredentialFormat } from '../dto/issue-credential.dto';

export interface SignResult {
  // For ldp_vc: the full signed JSON-LD credential.
  // For jwt_vc_json / vc+sd-jwt: an EnvelopedVerifiableCredential JSON wrapper.
  signed: W3CCredential | Record<string, any>;
  // Raw compact string for enveloped formats (null for ldp_vc).
  enveloped: string | null;
}

// Builds and signs a credential in the requested format.
// The ldp_vc branch is byte-for-byte the existing path (delegates to signVC);
// jwt_vc_json and vc+sd-jwt are additive branches that delegate to the new
// identity-service JWT/SD-JWT endpoints. This service never signs directly —
// all key material stays in identity-service / Vault.
@Injectable()
export class CredentialFormatService {
  private logger = new Logger(CredentialFormatService.name);

  constructor(private readonly identityUtilsService: IdentityUtilsService) {}

  async signInFormat(
    credInReq: W3CCredential,
    issuer: IssuerType,
    format: CredentialFormat = 'ldp_vc',
    opts: {
      disclosable?: string[];
      holderJwk?: Record<string, any>;
      holderKid?: string;
      docType?: string;
      namespaces?: Record<string, Record<string, any>>;
      vct?: string;
    } = {}
  ): Promise<SignResult> {
    switch (format) {
      case 'ldp_vc':
        return this.signLdp(credInReq, issuer);
      case 'jwt_vc_json':
        return this.signJwtVc(credInReq, issuer);
      case 'vc+sd-jwt':
        return this.signSdJwtVc(credInReq, issuer, opts);
      case 'mso_mdoc':
        return this.signMdoc(credInReq, issuer, opts);
      default:
        throw new InternalServerErrorException(`Unsupported format: ${format}`);
    }
  }

  private async signLdp(credInReq: W3CCredential, issuer: IssuerType): Promise<SignResult> {
    const signed = await this.identityUtilsService.signVC(
      credInReq as CredentialPayload,
      issuer
    );
    return { signed, enveloped: null };
  }

  private async signJwtVc(credInReq: W3CCredential, issuer: IssuerType): Promise<SignResult> {
    const subject = credInReq.credentialSubject as { id?: string };
    // W3C VC-JWT requires `nbf`/`exp` to represent vc.issuanceDate/
    // vc.expirationDate exactly. `nbf` is integer seconds, but our dates are
    // minted with millisecond precision (new Date().toISOString()), and strict
    // holders compare `Date.parse(vc.issuanceDate) / 1000` — unfloored —
    // against `nbf`. A credential carrying `...:12.345Z` therefore never
    // matches nbf `…12` and is rejected outright as a mismatch between the
    // JWT's `nbf` and the embedded `vc.issuanceDate`. Embed whole-second
    // dates in the `vc` claim so the two representations are equal by
    // construction.
    const nbf = this.toEpoch(credInReq.issuanceDate);
    const exp = credInReq.expirationDate
      ? this.toEpoch(credInReq.expirationDate)
      : undefined;
    const vc = {
      ...credInReq,
      issuanceDate: new Date(nbf * 1000).toISOString(),
      ...(exp !== undefined
        ? { expirationDate: new Date(exp * 1000).toISOString() }
        : {}),
    };
    const claims = {
      iss: (issuer as any)?.id || issuer,
      sub: subject?.id || undefined,
      nbf,
      ...(exp !== undefined ? { exp } : {}),
      jti: credInReq.id,
      vc,
    };
    const jwt = await this.identityUtilsService.signJwt(issuer, claims, { typ: 'JWT' });
    return { signed: this.envelope(credInReq.id, `data:application/vc+jwt,${jwt}`), enveloped: jwt };
  }

  private async signSdJwtVc(
    credInReq: W3CCredential,
    issuer: IssuerType,
    opts: {
      disclosable?: string[];
      holderJwk?: Record<string, any>;
      holderKid?: string;
      vct?: string;
    }
  ): Promise<SignResult> {
    const subject = (credInReq.credentialSubject || {}) as Record<string, any>;
    // Flatten subject claims to top level for SD-JWT selective disclosure.
    const disclosable =
      opts.disclosable ??
      Object.keys(subject).filter((k) => k !== 'id');
    const payload: Record<string, any> = {
      iss: (issuer as any)?.id || issuer,
      ...(subject.id ? { sub: subject.id } : {}),
      iat: this.toEpoch(credInReq.issuanceDate),
      ...(credInReq.expirationDate ? { exp: this.toEpoch(credInReq.expirationDate) } : {}),
      vct: opts.vct || (credInReq.type && credInReq.type[credInReq.type.length - 1]) || 'VerifiableCredential',
      jti: credInReq.id,
      ...subject,
      // Key binding must be expressed the same way the wallet requested it.
      // A DID-bound request (proof header carried a `kid`) has to be echoed as
      // `cnf.kid`: a conformant holder only accepts `cnf.jwk` for a request it
      // bound with a raw JWK, and otherwise rejects the credential outright as
      // issued for a key that wasn't in the credential request. Binding by
      // value stays the fallback for inline-jwk proofs, which have no kid.
      ...(opts.holderKid
        ? { cnf: { kid: opts.holderKid } }
        : opts.holderJwk
          ? { cnf: { jwk: opts.holderJwk } }
          : {}),
    };
    delete payload.id; // subject.id already mapped to sub
    const sdJwt = await this.identityUtilsService.signSdJwt(issuer, payload, disclosable, {});
    return { signed: this.envelope(credInReq.id, `data:application/vc+sd-jwt,${sdJwt}`), enveloped: sdJwt };
  }

  // mso_mdoc has no W3C credentialSubject shape at all — claims live under
  // {namespace: {element: value}}, supplied via opts.namespaces rather than
  // derived from credInReq.credentialSubject like every other format.
  private async signMdoc(
    credInReq: W3CCredential,
    issuer: IssuerType,
    opts: { holderJwk?: Record<string, any>; docType?: string; namespaces?: Record<string, Record<string, any>> }
  ): Promise<SignResult> {
    if (!opts.docType || !opts.namespaces) {
      throw new InternalServerErrorException('mso_mdoc requires docType and namespaces');
    }
    const mdoc = await this.identityUtilsService.signMdoc(
      issuer,
      opts.docType,
      opts.namespaces,
      opts.holderJwk,
    );
    return { signed: this.envelope(credInReq.id, `data:application/mdoc,${mdoc}`), enveloped: mdoc };
  }

  // W3C VC Data Model 2.0 EnvelopedVerifiableCredential wrapper — lets the
  // stored `signed` field stay a JSON object for all formats.
  private envelope(id: string, dataUri: string): Record<string, any> {
    return {
      '@context': ['https://www.w3.org/ns/credentials/v2'],
      id: dataUri,
      type: 'EnvelopedVerifiableCredential',
      credentialId: id,
    };
  }

  private toEpoch(dateStr?: string): number {
    if (!dateStr) return Math.floor(Date.now() / 1000);
    const t = new Date(dateStr).getTime();
    return isNaN(t) ? Math.floor(Date.now() / 1000) : Math.floor(t / 1000);
  }
}
