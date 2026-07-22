import {
  BadRequestException,
  Inject,
  Injectable,
  Logger,
  NotFoundException,
} from '@nestjs/common';
import { v4 as uuid } from 'uuid';
import * as crypto from 'crypto';
import { SESSION_STORE, SessionStore } from '../session/session-store.interface';
import { CredentialsClient } from '../clients/credentials.client';
import { SchemaClient } from '../clients/schema.client';
import { TokenService } from './token.service';
import { PopService } from './pop.service';
import { loadConfig } from '../config/configuration';

const PREAUTH_GRANT = 'urn:ietf:params:oauth:grant-type:pre-authorized_code';

interface OfferSession {
  credentialConfigurationId: string;
  format: string;
  schemaId: string;
  schemaVersion: string;
  claims: Record<string, any>;
  preAuthCode: string;
  txCodeRequired: boolean;
  tags: string[];
  // Optional deferred: when set, issuance waits on this claim id.
  deferredClaimId?: string;
}

// Core OID4VCI orchestration. Owns short-lived session state (offers, codes,
// nonces, deferred txns) and delegates signing/verification to existing services.
@Injectable()
export class Oid4vciService {
  private readonly logger = new Logger(Oid4vciService.name);
  private readonly config = loadConfig();

  constructor(
    @Inject(SESSION_STORE) private readonly store: SessionStore,
    private readonly credentials: CredentialsClient,
    private readonly schema: SchemaClient,
    private readonly tokens: TokenService,
    private readonly pop: PopService,
  ) {}

  // --- Metadata ------------------------------------------------------------

  async issuerMetadata() {
    const configs = await this.schema.getOid4vciConfigs();
    const supported: Record<string, any> = {};
    for (const cfg of configs) {
      for (const format of cfg.formats) {
        const id = cfg.formats.length > 1 ? `${cfg.name}_${format}` : cfg.name;
        supported[id] = {
          format,
          scope: cfg.name,
          cryptographic_binding_methods_supported: ['did:web', 'did:key', 'jwk'],
          credential_signing_alg_values_supported: ['ES256', 'Ed25519Signature2020'],
          proof_types_supported: { jwt: { proof_signing_alg_values_supported: ['ES256'] } },
          ...(format === 'vc+sd-jwt' ? { vct: cfg.vct } : {}),
          display: cfg.display,
          credential_definition: {
            type: ['VerifiableCredential', cfg.name],
          },
          // internal hint (not part of the spec response consumers care about)
          _schema: { id: cfg.schemaId, version: cfg.version, tags: cfg.tags },
        };
      }
    }

    const base = {
      credential_issuer: this.config.publicUrl,
      authorization_servers: [this.config.publicUrl],
      credential_endpoint: `${this.config.publicUrl}/oid4vc/credential`,
      nonce_endpoint: `${this.config.publicUrl}/oid4vc/nonce`,
      deferred_credential_endpoint: `${this.config.publicUrl}/oid4vc/deferred`,
      notification_endpoint: `${this.config.publicUrl}/oid4vc/notification`,
    };

    // Draft-13 (Inji) uses `credentials_supported`; final 1.0 uses
    // `credential_configurations_supported`.
    if (this.config.draft13CompatMode) {
      return { ...base, credentials_supported: supported };
    }
    return { ...base, credential_configurations_supported: supported };
  }

  // --- Offer ---------------------------------------------------------------

  async createOffer(body: {
    credential_configuration_id: string;
    claims: Record<string, any>;
    format?: string;
    tx_code_required?: boolean;
    tags?: string[];
    deferred_claim_id?: string;
  }) {
    const configs = await this.schema.getOid4vciConfigs();
    const cfg = configs.find(
      (c) =>
        c.name === body.credential_configuration_id ||
        `${c.name}_${body.format}` === body.credential_configuration_id,
    );
    if (!cfg) {
      throw new NotFoundException(
        `Credential configuration '${body.credential_configuration_id}' not enabled for OID4VCI`,
      );
    }
    const format = body.format || cfg.formats[0] || 'ldp_vc';
    if (!cfg.formats.includes(format)) {
      throw new BadRequestException(`Format '${format}' not supported for this credential`);
    }

    const id = uuid();
    const preAuthCode = this.randomToken();
    const session: OfferSession = {
      credentialConfigurationId: body.credential_configuration_id,
      format,
      schemaId: cfg.schemaId,
      schemaVersion: cfg.version,
      claims: body.claims || {},
      preAuthCode,
      txCodeRequired: !!body.tx_code_required,
      tags: body.tags || cfg.tags || [cfg.name],
      deferredClaimId: body.deferred_claim_id,
    };
    await this.store.set(`oid4vc:offer:${id}`, session, this.config.ttl.offer);
    // Index by pre-auth code for the token endpoint.
    await this.store.set(`oid4vc:code:${preAuthCode}`, { offerId: id }, this.config.ttl.offer);

    const offerObject = this.buildOfferObject(id, preAuthCode, session.txCodeRequired);
    const offerUri = `${this.config.publicUrl}/oid4vc/offer/${id}`;
    const qrData = `openid-credential-offer://?credential_offer_uri=${encodeURIComponent(offerUri)}`;

    return {
      offer_id: id,
      credential_offer_uri: offerUri,
      credential_offer: offerObject,
      qr_data: qrData,
    };
  }

  async getOffer(id: string) {
    const session = await this.store.get<OfferSession>(`oid4vc:offer:${id}`);
    if (!session) throw new NotFoundException('Offer not found or expired');
    return this.buildOfferObject(id, session.preAuthCode, session.txCodeRequired);
  }

  private buildOfferObject(id: string, preAuthCode: string, txCodeRequired: boolean) {
    const grant: any = { 'pre-authorized_code': preAuthCode };
    if (this.config.draft13CompatMode) {
      // draft-13 idiom
      grant.user_pin_required = txCodeRequired;
    } else if (txCodeRequired) {
      grant.tx_code = { input_mode: 'numeric', length: 6 };
    }
    return {
      credential_issuer: this.config.publicUrl,
      credential_configuration_ids: [id],
      grants: { [PREAUTH_GRANT]: grant },
    };
  }

  // --- Token ---------------------------------------------------------------

  async token(body: Record<string, any>) {
    const grantType = body.grant_type;
    if (grantType !== PREAUTH_GRANT) {
      throw new BadRequestException('unsupported_grant_type');
    }
    const code = body['pre-authorized_code'];
    if (!code) throw new BadRequestException('invalid_request: missing pre-authorized_code');

    // Atomic single-use consume of the code index.
    const codeEntry = await this.store.getdel<{ offerId: string }>(`oid4vc:code:${code}`);
    if (!codeEntry) throw new BadRequestException('invalid_grant: bad or used code');
    const session = await this.store.get<OfferSession>(`oid4vc:offer:${codeEntry.offerId}`);
    if (!session) throw new BadRequestException('invalid_grant: offer expired');

    // tx_code / user_pin check.
    if (session.txCodeRequired) {
      const pin = body.tx_code || body.user_pin;
      if (!pin) throw new BadRequestException('invalid_request: tx_code required');
      // NOTE: pin is compared to what the issuer distributed out-of-band; here
      // we accept any non-empty pin in dev. Wire real pin storage per deployment.
    }

    const accessToken = await this.tokens.mintAccessToken({
      sub: codeEntry.offerId,
      credential_configuration_id: session.credentialConfigurationId,
    });
    const cNonce = await this.issueNonce();

    return {
      access_token: accessToken,
      token_type: 'Bearer',
      expires_in: this.config.ttl.accessToken,
      c_nonce: cNonce,
      c_nonce_expires_in: this.config.ttl.nonce,
      // draft-13 wallets also read authorization_details / c_nonce here.
    };
  }

  // --- Nonce ---------------------------------------------------------------

  async issueNonce(): Promise<string> {
    const nonce = this.randomToken();
    await this.store.set(`oid4vc:nonce:${nonce}`, '1', this.config.ttl.nonce);
    return nonce;
  }

  // --- Credential ----------------------------------------------------------

  async credential(authHeader: string | undefined, body: Record<string, any>) {
    const tokenPayload = await this.tokens.validateAccessToken(authHeader);
    const offerId = tokenPayload.sub;
    const session = await this.store.get<OfferSession>(`oid4vc:offer:${offerId}`);
    if (!session) throw new BadRequestException('Offer session expired');

    // Verify holder proof-of-possession.
    const proofJwt = body?.proof?.jwt;
    if (!proofJwt) throw new BadRequestException('Missing proof.jwt');
    // Nonce inside the proof must be a live, single-use c_nonce.
    const proofClaims = this.decodeJwtClaims(proofJwt);
    const nonce = proofClaims?.nonce;
    const nonceValid = await this.store.getdel(`oid4vc:nonce:${nonce}`);
    if (!nonceValid) {
      throw new BadRequestException({ error: 'invalid_or_missing_proof', c_nonce: await this.issueNonce() });
    }
    const popResult = await this.pop.verifyJwtProof(proofJwt, {
      audience: this.config.publicUrl,
      nonce,
    });
    if (!popResult.valid) {
      throw new BadRequestException(`invalid_proof: ${popResult.error}`);
    }

    // Deferred: if the offer is tied to an unresolved claim, return a txn id.
    if (session.deferredClaimId && !(await this.isClaimReady(session.deferredClaimId))) {
      const txId = uuid();
      await this.store.set(
        `oid4vc:deferred:${txId}`,
        { offerId, holderDid: popResult.holderDid, holderJwk: popResult.holderJwk },
        this.config.ttl.deferred,
      );
      return { transaction_id: txId, c_nonce: await this.issueNonce() };
    }

    const credential = await this.issueForSession(session, popResult.holderDid, popResult.holderJwk);
    return { credential, c_nonce: await this.issueNonce(), format: session.format };
  }

  async deferred(authHeader: string | undefined, body: Record<string, any>) {
    await this.tokens.validateAccessToken(authHeader);
    const txId = body?.transaction_id;
    if (!txId) throw new BadRequestException('Missing transaction_id');
    const txn = await this.store.get<any>(`oid4vc:deferred:${txId}`);
    if (!txn) throw new NotFoundException('Unknown transaction_id');
    const session = await this.store.get<OfferSession>(`oid4vc:offer:${txn.offerId}`);
    if (!session) throw new BadRequestException('Offer session expired');

    if (session.deferredClaimId && !(await this.isClaimReady(session.deferredClaimId))) {
      // Still pending — spec: 202 with interval hint (handled in controller).
      return { pending: true, interval: 60 };
    }
    const credential = await this.issueForSession(session, txn.holderDid, txn.holderJwk);
    await this.store.del(`oid4vc:deferred:${txId}`);
    return { credential, format: session.format };
  }

  async notification(authHeader: string | undefined, body: Record<string, any>) {
    await this.tokens.validateAccessToken(authHeader);
    // MVP: log accept/deny telemetry only.
    this.logger.log(`OID4VCI notification: ${JSON.stringify(body)}`);
    return;
  }

  // --- Internal ------------------------------------------------------------

  private async issueForSession(
    session: OfferSession,
    holderDid?: string,
    holderJwk?: any,
  ) {
    const credential = {
      '@context': ['https://www.w3.org/2018/credentials/v1'],
      type: ['VerifiableCredential', session.credentialConfigurationId.split('_')[0]],
      issuer: this.tokens.getIssuerDid(),
      issuanceDate: new Date().toISOString(),
      credentialSubject: {
        ...(holderDid ? { id: holderDid } : {}),
        ...session.claims,
      },
    };
    const res = await this.credentials.issue({
      credential,
      credentialSchemaId: session.schemaId,
      credentialSchemaVersion: session.schemaVersion,
      tags: session.tags,
      format: session.format,
      holderJwk,
    });
    return res.credential;
  }

  // Hook point for deferred issuance backed by the attestation/claim workflow.
  // Default (no claim workflow wired): treat as ready.
  private async isClaimReady(_claimId: string): Promise<boolean> {
    return true;
  }

  private randomToken(): string {
    return crypto.randomBytes(24).toString('base64url');
  }

  private decodeJwtClaims(jwt: string): any {
    try {
      const [, payload] = jwt.split('.');
      return JSON.parse(Buffer.from(payload, 'base64url').toString('utf8'));
    } catch {
      return {};
    }
  }
}
