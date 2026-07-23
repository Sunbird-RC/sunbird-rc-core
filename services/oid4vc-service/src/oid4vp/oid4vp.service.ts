import {
  BadRequestException,
  ForbiddenException,
  Inject,
  Injectable,
  Logger,
  NotFoundException,
} from '@nestjs/common';
import { v4 as uuid } from 'uuid';
import * as crypto from 'crypto';
import { SESSION_STORE, SessionStore } from '../session/session-store.interface';
import { IdentityClient } from '../clients/identity.client';
import { CredentialsClient } from '../clients/credentials.client';
import { TokenService } from '../oid4vci/token.service';
import { DcqlService } from './dcql.service';
import { loadConfig } from '../config/configuration';
import * as jose from 'jose';

interface VpTxn {
  dcqlQuery: any;
  nonce: string;
  state: string;
  status: 'pending' | 'verified' | 'failed';
  result?: any;
}

// OID4VP verifier-role orchestration. Owns the VP transaction state and runs
// the full presentation validation chain, delegating VC signature checks to
// credentials-service and DID resolution to identity-service.
@Injectable()
export class Oid4vpService {
  private readonly logger = new Logger(Oid4vpService.name);
  private readonly config = loadConfig();

  constructor(
    @Inject(SESSION_STORE) private readonly store: SessionStore,
    private readonly identity: IdentityClient,
    private readonly credentials: CredentialsClient,
    private readonly tokens: TokenService,
    private readonly dcql: DcqlService,
  ) {}

  // Verifier creates a presentation request.
  async createRequest(body: { dcql_query: any }) {
    if (!body?.dcql_query) throw new BadRequestException('dcql_query required');
    const id = uuid();
    const nonce = crypto.randomBytes(24).toString('base64url');
    const state = crypto.randomBytes(16).toString('base64url');
    const responseUri = `${this.config.publicUrl}/vp/response`;

    // client_id / scheme history, condensed from live interop testing against
    // walt.id's wallet:
    //  1. Signed JAR with a `did:` client_id → walt.id: "UnsupportedPrefix -
    //     Client ID prefix 'did' is not supported."
    //  2. Unsigned, OID4VP-1.0-final-style prefixed client_id
    //     (`redirect_uri:${responseUri}`) → walt.id's own request parser threw
    //     a raw JsonDecodingException trying to parse that string, because
    //     walt.id targets an OLDER OID4VP draft where `client_id_scheme` is a
    //     SEPARATE request parameter and `client_id` itself is unprefixed —
    //     matching walt.id's own docs, which show `client_id_scheme=redirect_uri`
    //     as a standalone field (final-1.0 dropped this field in favor of the
    //     prefix-in-client_id convention walt.id doesn't yet implement).
    // Fix: emit the older-draft shape (unprefixed client_id + client_id_scheme)
    // since that's what a real tested wallet needs today. If a strictly
    // final-1.0 wallet needs the prefixed form instead, that's a genuine
    // spec-version fork worth a compat flag later — same pattern as
    // DRAFT13_COMPAT_MODE on the OID4VCI side — but not implemented until
    // there's a concrete wallet that needs it.
    const requestObject = {
      client_id: responseUri,
      client_id_scheme: 'redirect_uri',
      response_type: 'vp_token',
      response_mode: 'direct_post',
      response_uri: responseUri,
      nonce,
      state,
      dcql_query: body.dcql_query,
    };

    const txn: VpTxn = { dcqlQuery: body.dcql_query, nonce, state, status: 'pending' };
    await this.store.set(`oid4vp:txn:${id}`, { ...txn, requestObject }, this.config.ttl.vpTxn);
    // index by state so direct_post can find the txn
    await this.store.set(`oid4vp:state:${state}`, { id }, this.config.ttl.vpTxn);

    const requestUri = `${this.config.publicUrl}/vp/request-object/${id}`;
    const link = `openid4vp://?client_id=${encodeURIComponent(
      requestObject.client_id,
    )}&request_uri=${encodeURIComponent(requestUri)}`;

    return { transaction_id: id, request_uri: requestUri, qr_data: link };
  }

  async getRequestObject(id: string): Promise<any> {
    const txn = await this.store.get<any>(`oid4vp:txn:${id}`);
    if (!txn) throw new NotFoundException('VP request not found or expired');
    return txn.requestObject;
  }

  // direct_post: wallet submits the vp_token. Runs the validation chain.
  async submitResponse(body: Record<string, any>) {
    const state = body.state;
    if (!state) throw new BadRequestException('missing state');
    const idx = await this.store.get<{ id: string }>(`oid4vp:state:${state}`);
    if (!idx) throw new BadRequestException('unknown or expired state');
    const key = `oid4vp:txn:${idx.id}`;
    const txn = await this.store.get<any>(key);
    if (!txn || txn.status !== 'pending') {
      throw new BadRequestException('transaction not pending');
    }

    const checks: Record<string, string> = {};
    try {
      const vpToken = body.vp_token;
      if (!vpToken) throw new Error('missing vp_token');

      // 1. Decode the VP token (may be a JWT-VP or an array).
      const vpJwt = Array.isArray(vpToken) ? vpToken[0] : vpToken;
      const vpHeader = jose.decodeProtectedHeader(vpJwt);
      const vpClaims: any = jose.decodeJwt(vpJwt);

      // 2. Resolve holder DID + verify holder signature over the VP.
      const holderKid = vpHeader.kid as string;
      let holderDid = holderKid ? holderKid.split('#')[0] : vpClaims.iss;
      let holderPublicJwk: any = vpHeader.jwk as any;

      // did:jwk wallets (e.g. walt.id) commonly sign with an inline `jwk`
      // header and no `kid`/`iss`, or a self-contained `did:jwk:...` DID —
      // neither is resolvable via identity-service's registry, which only
      // knows its own DB plus did:web (see did.service.ts resolveDID: any
      // other method 404s). did:jwk is deterministic by spec — the DID
      // Document is just the base64url-decoded JWK embedded in the
      // identifier itself — so resolve it locally instead of round-tripping
      // to identity-service. Mirrors the same fallback already applied to
      // the issuance-side PoP check in pop.service.ts.
      if (!holderPublicJwk && holderDid?.startsWith('did:jwk:')) {
        try {
          holderPublicJwk = JSON.parse(
            Buffer.from(holderDid.slice('did:jwk:'.length), 'base64url').toString('utf8'),
          );
        } catch {
          throw new Error('malformed did:jwk holder DID');
        }
      }
      if (!holderPublicJwk) {
        const holderDidDoc = await this.identity.resolveDID(holderDid);
        const holderVm = (holderDidDoc.verificationMethod || []).find(
          (m: any) => (holderKid ? m.id === holderKid : true) && m.publicKeyJwk,
        );
        if (!holderVm) throw new Error('holder key not resolvable');
        holderPublicJwk = holderVm.publicKeyJwk;
      }
      if (!holderDid) {
        holderDid = `did:jwk:${Buffer.from(JSON.stringify(holderPublicJwk)).toString('base64url')}`;
      }
      const holderKey = await jose.importJWK(holderPublicJwk, (vpHeader.alg as string) || 'ES256');
      await jose.compactVerify(vpJwt, holderKey);
      checks.holderSignature = 'OK';

      // 3. Nonce + audience (replay protection).
      if (vpClaims.nonce !== txn.nonce) throw new Error('nonce mismatch');
      checks.nonce = 'OK';

      // 4. Extract the embedded VC(s).
      const vp = vpClaims.vp || vpClaims;
      const embedded = this.extractCredentials(vp);
      if (!embedded.length) throw new Error('no verifiable credentials in VP');

      // 5. Per-VC signature verify (delegated) + holder binding + status.
      //
      // Deliberately NOT passing {challenge: txn.nonce, domain: ...} here.
      // That was found live to break every ldp_vc presentation: the embedded
      // VC's proof is a static assertion signature created once at issuance
      // time, long before this (or any) presentation's nonce existed, so
      // credentials-service's checkChallengeDomain() would require an
      // impossible match and always fail proof:'OK'. Replay/freshness
      // protection for the PRESENTATION is already correctly enforced above
      // (step 3, the JWT-VP wrapper's own `nonce` claim check) — passing the
      // presentation's nonce down into the embedded credential's own
      // signature check applies that protection at the wrong layer.
      const presented: Array<any> = [];
      for (const vc of embedded) {
        const verifyRes = await this.credentials.verify(vc.raw);
        const proofOk = verifyRes?.checks?.[0]?.proof === 'OK';
        const notRevoked = verifyRes?.checks?.[0]?.revoked !== 'NOK';
        if (!proofOk) throw new Error('embedded VC signature invalid');
        if (!notRevoked) throw new Error('embedded VC revoked');

        // holder binding: subject id must equal the VP signer
        const subjectId = vc.claims?.id || vc.claims?.sub || vc.subjectId;
        if (subjectId && subjectId !== holderDid) {
          throw new Error('holder binding failed: subject != presenter');
        }
        presented.push({
          types: vc.types,
          vct: vc.vct,
          format: vc.format,
          claims: vc.claims,
        });
      }
      checks.credentialSignatures = 'OK';
      checks.holderBinding = 'OK';
      checks.revocation = 'OK';

      // 6. DCQL satisfaction.
      const dcqlResult = this.dcql.evaluate(txn.dcqlQuery, presented);
      if (!dcqlResult.satisfied) throw new Error(`DCQL not satisfied: ${dcqlResult.reason}`);
      checks.dcql = 'OK';

      // 7. Store the result.
      const result = { verified: true, checks, claims: dcqlResult.matched, holderDid };
      await this.store.set(key, { ...txn, status: 'verified', result }, this.config.ttl.vpTxn);
      return { redirect_uri: null, status: 'ok' };
    } catch (err) {
      this.logger.warn(`VP verification failed: ${err}`);
      const result = { verified: false, checks, error: `${err}` };
      await this.store.set(key, { ...txn, status: 'failed', result }, this.config.ttl.vpTxn);
      throw new ForbiddenException(result);
    }
  }

  async getStatus(id: string) {
    const txn = await this.store.get<VpTxn>(`oid4vp:txn:${id}`);
    if (!txn) throw new NotFoundException('VP transaction not found');
    return { status: txn.status, ...(txn.result || {}) };
  }

  // Pulls embedded credentials out of a VP, normalising the claim shape across
  // ldp_vc (JSON-LD object) and jwt_vc_json / vc+sd-jwt (compact strings).
  private extractCredentials(vp: any): Array<{
    raw: any;
    types: string[];
    vct?: string;
    format: string;
    claims: Record<string, any>;
    subjectId?: string;
  }> {
    let list = vp.verifiableCredential || vp.verifiable_credential || [];
    if (!Array.isArray(list)) list = [list];
    return list.map((vc: any) => {
      if (typeof vc === 'string') {
        // enveloped: jwt_vc_json or vc+sd-jwt
        const isSdJwt = vc.includes('~');
        if (isSdJwt) {
          // SD-JWT's whole point is that disclosed claim values do NOT live
          // in the signed JWS payload — identity-service's signSdJwt()
          // strips each disclosable claim out to a `_sd` digest and carries
          // the real [salt, name, value] only in the `~`-joined disclosure
          // segments (see jwt.service.ts). Previously this only decoded the
          // JWS part, so `claims` here was just `{iss, sub, vct, _sd, ...}`
          // with every actual disclosed value missing — DCQL claim-path
          // matching against a real SD-JWT presentation always failed.
          // Digest verification against `_sd` already happens in
          // credentials.verify() below; this reconstructs the claim view
          // for holder-binding/DCQL purposes the same way identity-service's
          // own verifySdJwt() does.
          const parts = vc.split('~');
          let claims: any = {};
          try {
            claims = jose.decodeJwt(parts[0]);
          } catch {
            claims = {};
          }
          const vct = claims.vct;
          delete claims._sd;
          delete claims._sd_alg;
          const disclosed: Record<string, any> = { ...claims };
          for (const d of parts.slice(1).filter((p) => p.length > 0)) {
            try {
              const [, name, value] = JSON.parse(
                Buffer.from(d, 'base64url').toString('utf8'),
              );
              disclosed[name] = value;
            } catch {
              // malformed disclosure — ignore, digest check in verify() below still gates trust
            }
          }
          return {
            raw: vc,
            types: ['VerifiableCredential'],
            vct,
            format: 'vc+sd-jwt',
            claims: disclosed,
            subjectId: disclosed?.sub,
          };
        }
        // jwt_vc_json: W3C VC-JWT convention, claims nested under `vc`.
        let claims: any = {};
        try {
          claims = jose.decodeJwt(vc);
        } catch {
          claims = {};
        }
        const inner = claims.vc || claims;
        const subject = inner.credentialSubject || claims;
        return {
          raw: vc,
          types: inner.type || ['VerifiableCredential'],
          format: 'jwt_vc_json',
          claims: subject,
          subjectId: subject?.id || claims.sub,
        };
      }
      // ldp_vc object
      const subject = vc.credentialSubject || {};
      return {
        raw: vc,
        types: vc.type || ['VerifiableCredential'],
        format: 'ldp_vc',
        claims: subject,
        subjectId: subject?.id,
      };
    });
  }
}
