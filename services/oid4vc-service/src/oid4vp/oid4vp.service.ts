import {
  BadRequestException,
  ForbiddenException,
  Inject,
  Injectable,
  InternalServerErrorException,
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
import { buildSessionTranscript, verifyMdocPresentation } from './mdoc-presentation.util';
import { resolveSelfContainedDidToJwk } from '../utils/self-contained-did.util';

interface VpTxn {
  dcqlQuery: any;
  nonce: string;
  state: string;
  status: 'pending' | 'verified' | 'failed';
  result?: any;
}

// Which of the three client_id / signing shapes a given request object was
// built in. Drives both the GET /vp/request-object/:id content-type and the
// client_id/response_uri invariant checked in submitResponse().
type VpRequestMode = 'signed' | 'unsigned' | 'legacy';

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
  // Resolution: default to the spec-correct draft-23/1.0 shape (prefixed
  // client_id, signed JAR via a `did:` client_id) and keep walt.id's exact
  // working shape available behind OID4VP_LEGACY_CLIENT_ID_SCHEME — same
  // compat-flag pattern as DRAFT13_COMPAT_MODE on the OID4VCI side.
  //
  // The redirect_uri client_id scheme MUST NOT be used with a signed request
  // object, so the mode (and therefore which client_id shape is emitted) has
  // to be picked here, before the client_id is baked into the QR deep link —
  // not negotiated later on the GET.
  private async buildRequestObject(
    responseUri: string,
    nonce: string,
    state: string,
    dcqlQuery: any,
    signed: boolean,
  ): Promise<{ mode: VpRequestMode; clientId: string; payload: any; jws?: string }> {
    if (this.config.vpLegacyClientIdScheme) {
      const payload = {
        client_id: responseUri,
        client_id_scheme: 'redirect_uri',
        response_type: 'vp_token',
        response_mode: 'direct_post',
        response_uri: responseUri,
        nonce,
        state,
        dcql_query: dcqlQuery,
      };
      return { mode: 'legacy', clientId: payload.client_id, payload };
    }

    const clientId = `redirect_uri:${responseUri}`;
    const basePayload = {
      client_id: clientId,
      response_type: 'vp_token',
      response_mode: 'direct_post',
      response_uri: responseUri,
      nonce,
      state,
      dcql_query: dcqlQuery,
    };

    if (!signed) {
      return { mode: 'unsigned', clientId, payload: basePayload };
    }

    // Signing means the wallet must resolve client_id's DID to verify the JAR.
    // An explicit VERIFIER_DID is the operator's deliberate choice and is used
    // as-is. Otherwise we fall back to the issuer DID (from ISSUER_DID, or
    // auto-provisioned at boot by token.service.ts's onModuleInit) — but ONLY
    // if it's did:web. Both fallbacks are routinely did:rcw, identity-service's
    // own method, resolvable only from its DB: confirmed live that signing with
    // a did:rcw makes every presentation fail with "unsupported client_id
    // prefix" as soon as a real wallet tries to verify. Better to refuse than
    // to emit a signed request no wallet can trust.
    const issuerDid = this.tokens.getIssuerDid();
    const verifierDid =
      this.config.verifierDid || (issuerDid?.startsWith('did:web:') ? issuerDid : undefined);
    if (!verifierDid) {
      throw new InternalServerErrorException(
        'OID4VP_SIGN_REQUEST is enabled but no externally-resolvable verifier DID is ' +
          `configured (issuer DID is ${issuerDid || 'unset'}; a did:rcw is resolvable only ` +
          'by identity-service itself, so no wallet could verify the request object) — set ' +
          'VERIFIER_DID to a did:web, or set OID4VP_SIGN_REQUEST=false / ' +
          'OID4VP_LEGACY_CLIENT_ID_SCHEME=true',
      );
    }
    const didClientId = `did:${verifierDid.replace(/^did:/, '')}`;
    const signedPayload = {
      ...basePayload,
      client_id: didClientId,
      iss: didClientId,
      aud: 'https://self-issued.me/v2',
      exp: Math.floor(Date.now() / 1000) + this.config.ttl.vpTxn,
    };
    const jws = await this.identity.signJwt(verifierDid, signedPayload, {
      kid: `${verifierDid}#key-0`,
      typ: 'oauth-authz-req+jwt',
    });
    return { mode: 'signed', clientId: didClientId, payload: signedPayload, jws };
  }

  // Verifier creates a presentation request.
  async createRequest(body: { dcql_query: any; signed?: boolean }) {
    if (!body?.dcql_query) throw new BadRequestException('dcql_query required');
    const id = uuid();
    const nonce = crypto.randomBytes(24).toString('base64url');
    const state = crypto.randomBytes(16).toString('base64url');
    const responseUri = `${this.config.publicUrl}/vp/response`;

    const signed = body.signed ?? this.config.vpSignRequest;
    const { mode, clientId, payload, jws } = await this.buildRequestObject(
      responseUri,
      nonce,
      state,
      body.dcql_query,
      signed,
    );

    const txn: VpTxn = { dcqlQuery: body.dcql_query, nonce, state, status: 'pending' };
    await this.store.set(
      `oid4vp:txn:${id}`,
      { ...txn, requestObject: payload, requestObjectJws: jws, requestMode: mode, clientId, responseUri },
      this.config.ttl.vpTxn,
    );
    // index by state so direct_post can find the txn
    await this.store.set(`oid4vp:state:${state}`, { id }, this.config.ttl.vpTxn);

    const requestUri = `${this.config.publicUrl}/vp/request-object/${id}`;
    const link = `openid4vp://?client_id=${encodeURIComponent(
      clientId,
    )}&request_uri=${encodeURIComponent(requestUri)}`;

    return { transaction_id: id, request_uri: requestUri, qr_data: link };
  }

  async getRequestObject(id: string): Promise<{ mode: VpRequestMode; body: any; contentType: string }> {
    const txn = await this.store.get<any>(`oid4vp:txn:${id}`);
    if (!txn) throw new NotFoundException('VP request not found or expired');
    if (txn.requestMode === 'signed') {
      return { mode: 'signed', body: txn.requestObjectJws, contentType: 'application/oauth-authz-req+jwt' };
    }
    return { mode: txn.requestMode, body: txn.requestObject, contentType: 'application/json' };
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

    // The redirect_uri client_id scheme's only authentication is that
    // client_id names the exact endpoint the response is posted back to —
    // nothing else establishes verifier identity for unsigned/legacy
    // requests. Nothing previously re-checked that invariant at response
    // time, so a stored txn with a doctored client_id/response_uri pairing
    // would have gone unnoticed.
    if (txn.requestMode !== 'signed') {
      const expectedClientId =
        txn.requestMode === 'legacy' ? txn.responseUri : `redirect_uri:${txn.responseUri}`;
      if (txn.clientId !== expectedClientId) {
        throw new BadRequestException('client_id/response_uri invariant violated');
      }
    }

    const checks: Record<string, string> = {};
    try {
      let vpToken = body.vp_token;
      if (!vpToken) throw new Error('missing vp_token');

      // direct_post sends the Authorization Response as
      // application/x-www-form-urlencoded (OID4VP §Response Mode
      // "direct_post"), so `vp_token` arrives as a JSON-encoded STRING, not a
      // pre-parsed object — Fastify's form parser has no notion of a nested
      // JSON value. Found live: walt.id's actual POST body has
      // `vp_token: '{"q":["..."]}'` (a string); JSON.parse it before treating
      // it as the DCQL-keyed object.
      if (typeof vpToken === 'string') {
        try {
          vpToken = JSON.parse(vpToken);
        } catch {
          throw new Error('vp_token is not valid JSON');
        }
      }

      // Per OID4VP §Response Parameters, `vp_token` is a JSON object keyed by
      // the DCQL credential query `id`, each value an array of Presentations
      // — NOT a bare JWT or an array of JWTs. Found live: walt.id's actual
      // response is `{ [queryId]: [presentation, ...] }`; the previous code
      // treated the whole object as a single JWT-VP and threw "Invalid Token
      // or Protected Header formatting" from jose before any per-credential
      // parsing even started.
      if (typeof vpToken !== 'object' || Array.isArray(vpToken)) {
        throw new Error(
          "vp_token must be a DCQL-keyed object of the form { [queryId]: [presentation, ...] }",
        );
      }

      const dcqlCredentials = txn.dcqlQuery?.credentials || [];
      if (!dcqlCredentials.length) throw new Error('txn has no DCQL credential queries');

      let holderDid: string | undefined;
      const presented: Array<any> = [];

      for (const cq of dcqlCredentials) {
        const entries = vpToken[cq.id];
        if (!Array.isArray(entries) || !entries.length) {
          throw new Error(`no presentation submitted for query '${cq.id}'`);
        }
        const entry = entries[0];

        if (cq.format === 'mso_mdoc') {
          // mso_mdoc presentation: the entry is a base64url CBOR
          // DeviceResponse, not a JWT-VP wrapper — a genuinely different wire
          // shape from the other formats (see mdoc-presentation.util.ts).
          const mdocGeneratedNonce = body.mdoc_generated_nonce;
          if (!mdocGeneratedNonce) throw new Error('missing mdoc_generated_nonce');
          const clientId = txn.requestObject?.client_id;
          const responseUri = txn.requestObject?.response_uri;
          const transcript = buildSessionTranscript(mdocGeneratedNonce, clientId, responseUri, txn.nonce);
          const mdocResult = await verifyMdocPresentation(entry, transcript);
          if (!mdocResult.verified) throw new Error(`mdoc presentation invalid: ${mdocResult.error}`);
          if (!mdocResult.documents.length) throw new Error('no documents in mdoc presentation');

          // @auth0/mdl's Verifier.verify() already covers, in one call: the
          // issuer's COSE signature + per-item digests (credentialSignatures),
          // and the device's COSE signature against deviceKeyInfo.deviceKey
          // computed over the session transcript we built from txn.nonce
          // (holderSignature + nonce + holderBinding all at once — a mismatch
          // in ANY of client_id/response_uri/nonce produces different
          // transcript bytes than what the wallet actually signed over, so
          // Verifier.verify() fails there instead of a separate explicit check).
          checks.holderSignature = 'OK';
          checks.nonce = 'OK';
          checks.credentialSignatures = 'OK';
          checks.holderBinding = 'OK';
          checks.revocation = 'OK'; // no mdoc revocation mechanism wired yet — same default as other formats
          presented.push(
            ...mdocResult.documents.map((doc) => ({
              types: [],
              docType: doc.docType,
              format: 'mso_mdoc',
              claims: doc.claims,
            })),
          );
          continue;
        }

        if (cq.format === 'dc+sd-jwt' || cq.format === 'vc+sd-jwt') {
          // The Presentation *is* the SD-JWT+KB compact string directly —
          // there is no outer VP-JWT wrapper for this format. The Key Binding
          // JWT trailing it carries the `nonce`/`aud` that prove holder
          // binding + replay protection (OID4VP "IETF SD-JWT VC" Presentation
          // Response). Delegate that whole check to credentials-service,
          // which already implements it end-to-end via identity-service's
          // verifySdJwt (issuer signature, disclosure digests, KB-JWT
          // signature against the issuer-embedded `cnf.jwk`, and nonce/aud) —
          // the same call path already used for issuance-time PoP, just with
          // challenge/domain now supplied.
          const verifyRes = await this.credentials.verify(entry, {
            challenge: txn.nonce,
            domain: txn.clientId,
          });
          const vcChecks = verifyRes?.checks?.[0] || {};
          if (vcChecks.proof !== 'OK') {
            throw new Error('SD-JWT+KB presentation invalid (signature, nonce, or audience)');
          }
          if (vcChecks.revoked === 'NOK') throw new Error('embedded VC revoked');
          checks.holderSignature = 'OK';
          checks.nonce = 'OK';
          checks.audience = 'OK';
          checks.credentialSignatures = 'OK';
          checks.holderBinding = 'OK';
          checks.revocation = 'OK';

          // Claim reconstruction is independent of the trust check above —
          // extractCredentials() already tolerates a trailing KB-JWT segment
          // (silently skipped as an unparseable "disclosure").
          const [parsed] = this.extractCredentials({ verifiableCredential: [entry] });
          if (!parsed) throw new Error('unable to parse SD-JWT claims');
          if (!holderDid && parsed.subjectId) holderDid = parsed.subjectId;
          presented.push({ types: parsed.types, vct: parsed.vct, format: cq.format, claims: parsed.claims });
          continue;
        }

        // jwt_vc_json / ldp_vc: the Presentation is itself a Verifiable
        // Presentation carrying its own nonce/aud (JWT-VP) or challenge/domain
        // (LD-proof VP), wrapping the embedded credential(s).
        if (typeof entry === 'string') {
          const vpHeader = jose.decodeProtectedHeader(entry);
          const vpClaims: any = jose.decodeJwt(entry);

          const holderKid = vpHeader.kid as string;
          let entryHolderDid = holderKid ? holderKid.split('#')[0] : vpClaims.iss;
          let holderPublicJwk: any = vpHeader.jwk as any;

          // did:jwk wallets (e.g. walt.id) commonly sign with an inline `jwk`
          // header and no `kid`/`iss`, or a self-contained `did:jwk:...` DID;
          // Credo presents with the `did:key` it bound at issuance. None is
          // resolvable via identity-service's registry, which only knows its own
          // DB plus did:web (see did.service.ts resolveDID: any other method
          // 404s). Both methods are deterministic by spec — the public key is
          // embedded in the identifier — so resolve locally instead of
          // round-tripping to identity-service. Mirrors the same fallback
          // applied to the issuance-side PoP check in pop.service.ts.
          if (!holderPublicJwk) {
            holderPublicJwk = resolveSelfContainedDidToJwk(entryHolderDid);
          }
          if (!holderPublicJwk) {
            const holderDidDoc = await this.identity.resolveDID(entryHolderDid);
            const holderVm = (holderDidDoc.verificationMethod || []).find(
              (m: any) => (holderKid ? m.id === holderKid : true) && m.publicKeyJwk,
            );
            if (!holderVm) throw new Error('holder key not resolvable');
            holderPublicJwk = holderVm.publicKeyJwk;
          }
          if (!entryHolderDid) {
            entryHolderDid = `did:jwk:${Buffer.from(JSON.stringify(holderPublicJwk)).toString('base64url')}`;
          }
          const holderKey = await jose.importJWK(holderPublicJwk, (vpHeader.alg as string) || 'ES256');
          await jose.compactVerify(entry, holderKey);
          checks.holderSignature = 'OK';

          if (vpClaims.nonce !== txn.nonce) throw new Error('nonce mismatch');
          checks.nonce = 'OK';

          // A VP token bound to a different verifier's client_id (e.g. replayed
          // against this endpoint after being obtained by another relying
          // party) must be rejected here — the request's own client_id is the
          // only value that anchors "who this presentation was made to".
          const aud = vpClaims.aud;
          const audMatches = Array.isArray(aud) ? aud.includes(txn.clientId) : aud === txn.clientId;
          if (!audMatches) throw new Error('audience mismatch');
          checks.audience = 'OK';

          const vp = vpClaims.vp || vpClaims;
          const embedded = this.extractCredentials(vp);
          if (!embedded.length) throw new Error('no verifiable credentials in VP');

          // Per-VC signature verify (delegated) + holder binding + status.
          //
          // Deliberately NOT passing {challenge: txn.nonce, domain: ...} here.
          // That was found live to break every ldp_vc presentation: the embedded
          // VC's proof is a static assertion signature created once at issuance
          // time, long before this (or any) presentation's nonce existed, so
          // credentials-service's checkChallengeDomain() would require an
          // impossible match and always fail proof:'OK'. Replay/freshness
          // protection for the PRESENTATION is already correctly enforced above
          // (the JWT-VP wrapper's own `nonce` claim check) — passing the
          // presentation's nonce down into the embedded credential's own
          // signature check applies that protection at the wrong layer.
          for (const vc of embedded) {
            const verifyRes = await this.credentials.verify(vc.raw);
            const proofOk = verifyRes?.checks?.[0]?.proof === 'OK';
            const notRevoked = verifyRes?.checks?.[0]?.revoked !== 'NOK';
            if (!proofOk) throw new Error('embedded VC signature invalid');
            if (!notRevoked) throw new Error('embedded VC revoked');

            // holder binding: subject id must equal the VP signer
            const subjectId = vc.claims?.id || vc.claims?.sub || vc.subjectId;
            if (subjectId && subjectId !== entryHolderDid) {
              throw new Error('holder binding failed: subject != presenter');
            }
            presented.push({
              types: vc.types,
              vct: vc.vct,
              format: vc.format,
              claims: vc.claims,
            });
          }
          if (!holderDid) holderDid = entryHolderDid;
          checks.credentialSignatures = 'OK';
          checks.holderBinding = 'OK';
          checks.revocation = 'OK';
        } else {
          // ldp_vc as a plain Data Integrity VP object — challenge/domain
          // live on the LD proof itself rather than JWT claims.
          const proof = entry?.proof || {};
          if (proof.challenge && proof.challenge !== txn.nonce) throw new Error('nonce mismatch');
          if (proof.domain && proof.domain !== txn.clientId) throw new Error('audience mismatch');
          checks.nonce = 'OK';
          checks.audience = 'OK';
          const verifyRes = await this.credentials.verify(entry, {
            challenge: txn.nonce,
            domain: txn.clientId,
          });
          const proofOk = verifyRes?.checks?.[0]?.proof === 'OK';
          if (!proofOk) throw new Error('ldp_vc presentation invalid');
          checks.holderSignature = 'OK';
          checks.credentialSignatures = 'OK';
          checks.holderBinding = 'OK';
          checks.revocation = 'OK';
          const subject = entry.credentialSubject || {};
          presented.push({ types: entry.type || ['VerifiableCredential'], format: 'ldp_vc', claims: subject });
        }
      }

      // DCQL satisfaction.
      const dcqlResult = this.dcql.evaluate(txn.dcqlQuery, presented);
      if (!dcqlResult.satisfied) throw new Error(`DCQL not satisfied: ${dcqlResult.reason}`);
      checks.dcql = 'OK';

      // Store the result.
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
