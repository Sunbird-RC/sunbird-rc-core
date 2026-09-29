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
import { PexService } from './pex.service';
import { resolveJsonPath } from './jsonpath.util';
import { loadConfig } from '../config/configuration';
import * as jose from 'jose';
import { buildSessionTranscript, verifyMdocPresentation } from './mdoc-presentation.util';
import {
  resolveSelfContainedDidToJwk,
  isSelfContainedDid,
  jwkPublicKeyEquals,
} from '../utils/self-contained-did.util';

interface VpTxn {
  queryMode: 'dcql' | 'pex';
  dcqlQuery?: any;
  presentationDefinition?: any;
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
    private readonly pex: PexService,
  ) {}

  // client_id / scheme history, condensed from live interop testing against
  // wallets implementing OID4VP drafts of varying vintage:
  //  1. Signed JAR with a `did:` client_id → rejected by wallets that don't
  //     support the `did` client_id prefix scheme.
  //  2. Unsigned, OID4VP-1.0-final-style prefixed client_id
  //     (`redirect_uri:${responseUri}`) → rejected by wallets targeting an
  //     OLDER OID4VP draft where `client_id_scheme` is a SEPARATE request
  //     parameter and `client_id` itself is unprefixed (final-1.0 dropped
  //     that field in favor of the prefix-in-client_id convention, which
  //     such wallets don't yet implement).
  // Resolution: default to the spec-correct draft-23/1.0 shape (prefixed
  // client_id, signed JAR via a `did:` client_id) and keep the older
  // separate-`client_id_scheme` shape available behind
  // OID4VP_LEGACY_CLIENT_ID_SCHEME — same compat-flag pattern as
  // DRAFT13_COMPAT_MODE on the OID4VCI side.
  //
  // The redirect_uri client_id scheme MUST NOT be used with a signed request
  // object, so the mode (and therefore which client_id shape is emitted) has
  // to be picked here, before the client_id is baked into the QR deep link —
  // not negotiated later on the GET.
  private async buildRequestObject(
    responseUri: string,
    nonce: string,
    state: string,
    queryPayload: Record<string, any>,
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
        ...queryPayload,
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
      ...queryPayload,
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

  // Verifier creates a presentation request — DCQL or PEX, mutually exclusive.
  async createRequest(body: {
    dcql_query?: any;
    presentation_definition?: any;
    signed?: boolean;
    client_metadata?: any;
  }) {
    const hasDcql = !!body?.dcql_query;
    const hasPex = !!body?.presentation_definition;
    if (hasDcql === hasPex) {
      throw new BadRequestException('exactly one of dcql_query or presentation_definition is required');
    }
    const queryMode: 'dcql' | 'pex' = hasDcql ? 'dcql' : 'pex';
    const queryPayload = {
      ...(hasDcql ? { dcql_query: body.dcql_query } : { presentation_definition: body.presentation_definition }),
      ...(body.client_metadata ? { client_metadata: body.client_metadata } : {}),
    };

    const id = uuid();
    const nonce = crypto.randomBytes(24).toString('base64url');
    const state = crypto.randomBytes(16).toString('base64url');
    const responseUri = `${this.config.publicUrl}/vp/response`;

    const signed = body.signed ?? this.config.vpSignRequest;
    const { mode, clientId, payload, jws } = await this.buildRequestObject(
      responseUri,
      nonce,
      state,
      queryPayload,
      signed,
    );

    const txn: VpTxn = {
      queryMode,
      ...(hasDcql ? { dcqlQuery: body.dcql_query } : { presentationDefinition: body.presentation_definition }),
      nonce,
      state,
      status: 'pending',
    };
    await this.store.set(
      `oid4vp:txn:${id}`,
      { ...txn, requestObject: payload, requestObjectJws: jws, requestMode: mode, clientId, responseUri },
      this.config.ttl.vpTxn,
    );
    // index by state so direct_post can find the txn
    await this.store.set(`oid4vp:state:${state}`, { id }, this.config.ttl.vpTxn);

    const requestUri = `${this.config.publicUrl}/vp/request-object/${id}`;
    const link =
      mode === 'signed'
        ? `openid4vp://authorize?client_id=${encodeURIComponent(clientId)}&request=${encodeURIComponent(jws!)}`
        : `openid4vp://authorize?${new URLSearchParams(
            Object.fromEntries(
              Object.entries(payload).map(([k, v]) => [k, typeof v === 'string' ? v : JSON.stringify(v)]),
            ),
          ).toString()}`;

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
      if (!body.vp_token) throw new Error('missing vp_token');

      let holderDid: string | undefined;
      const presented: Array<any> = [];
      let matched: Record<string, any> = {};

      if (txn.queryMode === 'pex') {
        const vpTokenArr = this.normalizePexVpToken(body.vp_token);
        let submission = body.presentation_submission;
        if (typeof submission === 'string') {
          try {
            submission = JSON.parse(submission);
          } catch {
            throw new Error('presentation_submission is not valid JSON');
          }
        }
        if (!submission || !Array.isArray(submission.descriptor_map)) {
          throw new Error('missing or invalid presentation_submission');
        }

        const descriptors = txn.presentationDefinition?.input_descriptors || [];
        for (const dm of submission.descriptor_map) {
          const descriptor = descriptors.find((d: any) => d.id === dm.id);
          if (!descriptor) throw new Error(`unknown descriptor id '${dm.id}' in presentation_submission`);
          const resolved = this.resolveDescriptorMapEntry(vpTokenArr, dm);
          const { presented: push, holderDid: hd } = await this.verifyPresentationEntry(
            resolved.value,
            resolved.format,
            txn,
            body,
            checks,
          );
          presented.push(...push);
          if (!holderDid && hd) holderDid = hd;
        }

        const pexResult = this.pex.evaluate(txn.presentationDefinition, presented);
        if (!pexResult.satisfied) throw new Error(`PEX not satisfied: ${pexResult.reason}`);
        checks.pex = 'OK';
        matched = pexResult.matched;
      } else {
        let vpToken = body.vp_token;

        // direct_post sends the Authorization Response as
        // application/x-www-form-urlencoded (OID4VP §Response Mode
        // "direct_post"), so `vp_token` arrives as a JSON-encoded STRING, not
        // a pre-parsed object — Fastify's form parser has no notion of a
        // nested JSON value. A real wallet's POST body has
        // `vp_token: '{"q":["..."]}'` (a string); JSON.parse it before
        // treating it as the DCQL-keyed object.
        if (typeof vpToken === 'string') {
          try {
            vpToken = JSON.parse(vpToken);
          } catch {
            throw new Error('vp_token is not valid JSON');
          }
        }

        // Per OID4VP §Response Parameters, `vp_token` is a JSON object keyed
        // by the DCQL credential query `id`, each value an array of
        // Presentations — NOT a bare JWT or an array of JWTs. A real
        // wallet's response is `{ [queryId]: [presentation, ...] }`;
        // treating the whole object as a single JWT-VP instead throws a
        // token/header parsing error before any per-credential parsing even
        // starts.
        if (typeof vpToken !== 'object' || Array.isArray(vpToken)) {
          throw new Error(
            "vp_token must be a DCQL-keyed object of the form { [queryId]: [presentation, ...] }",
          );
        }

        const dcqlCredentials = txn.dcqlQuery?.credentials || [];
        if (!dcqlCredentials.length) throw new Error('txn has no DCQL credential queries');

        for (const cq of dcqlCredentials) {
          const entries = vpToken[cq.id];
          if (!Array.isArray(entries) || !entries.length) {
            throw new Error(`no presentation submitted for query '${cq.id}'`);
          }
          const { presented: push, holderDid: hd } = await this.verifyPresentationEntry(
            entries[0],
            cq.format,
            txn,
            body,
            checks,
          );
          presented.push(...push);
          if (!holderDid && hd) holderDid = hd;
        }

        const dcqlResult = this.dcql.evaluate(txn.dcqlQuery, presented);
        if (!dcqlResult.satisfied) throw new Error(`DCQL not satisfied: ${dcqlResult.reason}`);
        checks.dcql = 'OK';
        matched = dcqlResult.matched;
      }

      // Store the result.
      const result = { verified: true, checks, claims: matched, holderDid };
      await this.store.set(key, { ...txn, status: 'verified', result }, this.config.ttl.vpTxn);
      return { redirect_uri: null, status: 'ok' };
    } catch (err) {
      this.logger.warn(`VP verification failed: ${err}`);
      const result = { verified: false, checks, error: `${err}` };
      await this.store.set(key, { ...txn, status: 'failed', result }, this.config.ttl.vpTxn);
      throw new ForbiddenException(result);
    }
  }

  // Verifies one presented entry (one credential/presentation, already
  // resolved from either a DCQL credential-query slot or a PEX
  // descriptor_map entry) and returns the presented[]-shaped items it
  // yields plus any holder DID it establishes. Shared by both query modes —
  // the per-format checks below don't care which query language selected
  // this entry.
  private async verifyPresentationEntry(
    entry: any,
    format: string,
    txn: any,
    body: any,
    checks: Record<string, string>,
  ): Promise<{ presented: any[]; holderDid?: string }> {
    if (format === 'mso_mdoc') {
      // mso_mdoc presentation: the entry is a base64url CBOR DeviceResponse,
      // not a JWT-VP wrapper — a genuinely different wire shape from the
      // other formats (see mdoc-presentation.util.ts).
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
      return {
        presented: mdocResult.documents.map((doc) => ({
          types: [],
          docType: doc.docType,
          format: 'mso_mdoc',
          claims: doc.claims,
        })),
      };
    }

    if (format === 'dc+sd-jwt' || format === 'vc+sd-jwt') {
      // The Presentation *is* the SD-JWT+KB compact string directly — there
      // is no outer VP-JWT wrapper for this format. The Key Binding JWT
      // trailing it carries the `nonce`/`aud` that prove holder binding +
      // replay protection (OID4VP "IETF SD-JWT VC" Presentation Response).
      // Delegate that whole check to credentials-service, which already
      // implements it end-to-end via identity-service's verifySdJwt (issuer
      // signature, disclosure digests, KB-JWT signature against the
      // issuer-embedded `cnf.jwk`, and nonce/aud) — the same call path
      // already used for issuance-time PoP, just with challenge/domain now
      // supplied.
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
      return {
        presented: [{ types: parsed.types, vct: parsed.vct, format, claims: parsed.claims }],
        holderDid: parsed.subjectId,
      };
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

      // did:jwk wallets commonly sign with an inline `jwk` header and no
      // `kid`/`iss`, or a self-contained `did:jwk:...` DID; others present
      // with the `did:key` they bound at issuance. Neither is resolvable
      // via identity-service's registry, which only knows its own DB plus
      // did:web (see did.service.ts resolveDID: any other method 404s).
      // Both methods are deterministic by spec — the public key is
      // embedded in the identifier — so resolve locally instead of
      // round-tripping to identity-service. Mirrors the same fallback
      // applied to the issuance-side PoP check in pop.service.ts.
      //
      // An inline `jwk` header is self-asserted; if `kid`/`iss` also
      // claims a holder DID, that DID's actual key — not the header —
      // must be trusted. Verify the two agree for self-contained DIDs;
      // reject an inline jwk alongside any registry-resolved DID method
      // outright, since its real key can only come from resolution.
      // Otherwise the holder-binding check below (subjectId ===
      // entryHolderDid) compares the embedded VC's subject against a DID
      // the presenter never actually proved control of.
      if (holderPublicJwk && entryHolderDid) {
        if (isSelfContainedDid(entryHolderDid)) {
          if (!jwkPublicKeyEquals(resolveSelfContainedDidToJwk(entryHolderDid), holderPublicJwk)) {
            throw new Error('holder DID does not match inline jwk header');
          }
        } else {
          throw new Error('inline jwk header not permitted alongside a registry-resolved holder DID');
        }
      }
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
      const presented: any[] = [];
      for (const vc of embedded) {
        const verifyRes = await this.credentials.verify(vc.raw);
        const proofOk = verifyRes?.checks?.[0]?.proof === 'OK';
        const notRevoked = verifyRes?.checks?.[0]?.revoked !== 'NOK';
        if (!proofOk) throw new Error('embedded VC signature invalid');
        if (!notRevoked) throw new Error('embedded VC revoked');

        // holder binding: subject id must equal the VP signer. Fail closed
        // (rather than skip) when the embedded VC carries no subject id at
        // all — otherwise a credential with no credentialSubject.id/sub
        // would silently report holderBinding: 'OK' with nothing actually
        // compared.
        const subjectId = vc.claims?.id || vc.claims?.sub || vc.subjectId;
        if (!subjectId || subjectId !== entryHolderDid) {
          throw new Error('holder binding failed: missing or mismatched subject id');
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
      return { presented, holderDid: entryHolderDid };
    }

    // ldp_vc as a full Data Integrity VerifiablePresentation object — `entry`
    // is the VP wrapper, not the credential (confirmed live against a real
    // wallet: it submits { type: ['VerifiablePresentation'], holder,
    // verifiableCredential: [...], proof: { challenge, domain, proofPurpose:
    // 'authentication', ... } }). The VP's own holder-binding proof carries
    // challenge/domain; each embedded VC has its own separate (static,
    // issuance-time, no challenge/domain) assertion proof — same split the
    // jwt_vc_json/ldp_vc-JWT branch above already applies via extractCredentials().
    const holderDid: string | undefined = (
      typeof entry.holder === 'string' ? entry.holder : entry.holder?.id
    )?.split('#')[0];
    const proof = entry?.proof || {};
    if (proof.challenge && proof.challenge !== txn.nonce) throw new Error('nonce mismatch');
    if (proof.domain && proof.domain !== txn.clientId) throw new Error('audience mismatch');
    checks.nonce = 'OK';
    checks.audience = 'OK';

    // Verifies the VP's OWN proof (holder-binding, `authentication` purpose)
    // — not any embedded VC's assertion proof.
    const vpVerifyRes = await this.credentials.verify(entry, {
      challenge: txn.nonce,
      domain: txn.clientId,
    });
    if (vpVerifyRes?.checks?.[0]?.proof !== 'OK') throw new Error('ldp_vc presentation invalid');
    checks.holderSignature = 'OK';

    const embedded = this.extractCredentials(entry);
    if (!embedded.length) throw new Error('no verifiable credentials in VP');

    const presented: any[] = [];
    for (const vc of embedded) {
      // Deliberately NOT passing {challenge, domain} here — same reasoning
      // as the jwt_vc_json/ldp_vc-JWT branch above: the embedded VC's proof
      // is a static assertion signature from issuance time, long before this
      // presentation's nonce existed.
      const verifyRes = await this.credentials.verify(vc.raw);
      const proofOk = verifyRes?.checks?.[0]?.proof === 'OK';
      const notRevoked = verifyRes?.checks?.[0]?.revoked !== 'NOK';
      if (!proofOk) throw new Error('embedded VC signature invalid');
      if (!notRevoked) throw new Error('embedded VC revoked');

      const subjectId = vc.claims?.id || vc.claims?.sub || vc.subjectId;
      if (!subjectId || subjectId !== holderDid) {
        throw new Error('holder binding failed: missing or mismatched subject id');
      }
      presented.push({ types: vc.types, vct: vc.vct, format: vc.format, claims: vc.claims });
    }
    checks.credentialSignatures = 'OK';
    checks.holderBinding = 'OK';
    checks.revocation = 'OK';
    return { presented, holderDid };
  }

  // PEX's vp_token wire shape differs from DCQL's: with exactly one
  // presentation, vp_token is the raw presentation itself — for
  // string-shaped formats (compact JWT-VP, SD-JWT+KB, base64url mdoc) that
  // means it is NOT JSON at all, so JSON.parse failing here means "bare
  // compact-string presentation," not an error (unlike DCQL's vp_token,
  // which is always a JSON-encoded object).
  private normalizePexVpToken(raw: any): any[] {
    let v = raw;
    if (typeof v === 'string') {
      try {
        v = JSON.parse(v);
      } catch {
        return [raw];
      }
    }
    return Array.isArray(v) ? v : [v];
  }

  // Resolves one presentation_submission descriptor_map entry (path +
  // optional path_nested chain) against the normalized vp_token array.
  private resolveDescriptorMapEntry(vpTokenArr: any[], dm: any): { value: any; format: string } {
    let value = this.resolveTopLevelPath(vpTokenArr, dm.path);
    if (value === undefined) throw new Error(`presentation_submission path '${dm.path}' did not resolve`);
    let format = dm.format;
    let nested = dm.path_nested;

    if (format === 'ldp_vp') return { value, format: 'ldp_vc' };

    // ponytail: no depth cap beyond the chain's own length — a definition
    // author controls their own definition's nesting depth.
    while (nested) {
      if (format === 'mso_mdoc' || format === 'vc+sd-jwt' || format === 'dc+sd-jwt') {
        throw new Error(`path_nested is not supported for format '${format}'`);
      }
      const decoded = this.decodeForTraversal(value);
      value = resolveJsonPath(decoded, nested.path);
      if (value === undefined) throw new Error(`presentation_submission path_nested '${nested.path}' did not resolve`);
      format = nested.format || format;
      nested = nested.path_nested;
    }
    return { value, format };
  }

  // descriptor_map[].path only ever needs to index the top-level vp_token
  // array — `$` for the single-presentation case, `$[n]` for the multiple
  // case. Deeper paths belong to path_nested, resolved separately against
  // the presentation's own decoded content.
  private resolveTopLevelPath(vpTokenArr: any[], path: string): any {
    if (path === '$') return vpTokenArr[0];
    const m = /^\$\[(\d+)\]$/.exec(path);
    if (m) return vpTokenArr[Number(m[1])];
    throw new Error(`unsupported presentation_submission path: ${path}`);
  }

  // Decodes one layer of a W3C VP wrapper so path_nested can descend into
  // its embedded credentials — compact JWT-VP -> its claims payload, plain
  // ldp_vc VP object -> itself. mso_mdoc/SD-JWT+KB are rejected before
  // reaching here (path_nested doesn't apply to non-VP-wrapping formats).
  private decodeForTraversal(entry: any): any {
    if (typeof entry === 'string') {
      const claims: any = jose.decodeJwt(entry);
      return claims.vp || claims;
    }
    return entry;
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
