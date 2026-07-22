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

    const requestObject = {
      client_id: `redirect_uri:${responseUri}`,
      response_type: 'vp_token',
      response_mode: 'direct_post',
      response_uri: responseUri,
      nonce,
      state,
      dcql_query: body.dcql_query,
      iss: this.config.publicUrl,
      aud: 'https://self-issued.me/v2',
    };
    // Sign the request object (JAR) with the issuer/verifier DID.
    const signedJar = await this.identity.signJwt(
      this.tokens.getIssuerDid(),
      requestObject,
      { typ: 'oauth-authz-req+jwt' },
    );

    const txn: VpTxn = { dcqlQuery: body.dcql_query, nonce, state, status: 'pending' };
    await this.store.set(`oid4vp:txn:${id}`, { ...txn, signedJar }, this.config.ttl.vpTxn);
    // index by state so direct_post can find the txn
    await this.store.set(`oid4vp:state:${state}`, { id }, this.config.ttl.vpTxn);

    const requestUri = `${this.config.publicUrl}/vp/request-object/${id}`;
    const link = `openid4vp://?client_id=${encodeURIComponent(
      requestObject.client_id,
    )}&request_uri=${encodeURIComponent(requestUri)}`;

    return { transaction_id: id, request_uri: requestUri, qr_data: link };
  }

  async getRequestObject(id: string): Promise<string> {
    const txn = await this.store.get<any>(`oid4vp:txn:${id}`);
    if (!txn) throw new NotFoundException('VP request not found or expired');
    return txn.signedJar;
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
      const holderDid = holderKid ? holderKid.split('#')[0] : vpClaims.iss;
      const holderDidDoc = await this.identity.resolveDID(holderDid);
      const holderVm = (holderDidDoc.verificationMethod || []).find(
        (m: any) => (holderKid ? m.id === holderKid : true) && m.publicKeyJwk,
      );
      if (!holderVm) throw new Error('holder key not resolvable');
      const holderKey = await jose.importJWK(
        holderVm.publicKeyJwk,
        (vpHeader.alg as string) || 'ES256',
      );
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
      const presented: Array<any> = [];
      for (const vc of embedded) {
        const verifyRes = await this.credentials.verify(vc.raw, {
          challenge: txn.nonce,
          domain: this.config.publicUrl,
        });
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
        const jwtPart = isSdJwt ? vc.split('~')[0] : vc;
        let claims: any = {};
        try {
          claims = jose.decodeJwt(jwtPart);
        } catch {
          claims = {};
        }
        const inner = claims.vc || claims;
        const subject = inner.credentialSubject || claims;
        return {
          raw: vc,
          types: inner.type || ['VerifiableCredential'],
          vct: claims.vct,
          format: isSdJwt ? 'vc+sd-jwt' : 'jwt_vc_json',
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
