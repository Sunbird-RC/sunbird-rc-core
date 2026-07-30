// Typed access to environment configuration for oid4vc-service.
export interface Oid4vcConfig {
  port: number;
  publicUrl: string;
  credentialServiceBaseUrl: string;
  identityBaseUrl: string;
  schemaBaseUrl: string;
  sessionStore: 'memory' | 'redis';
  redisUrl: string;
  issuerDid: string;
  oid4vpEnabled: boolean;
  draft13CompatMode: boolean;
  vpSignRequest: boolean;
  vpLegacyClientIdScheme: boolean;
  verifierDid: string;
  ttl: {
    offer: number;
    nonce: number;
    accessToken: number;
    deferred: number;
    vpTxn: number;
  };
}

const num = (v: string | undefined, def: number) => {
  const n = parseInt(v || '', 10);
  return isNaN(n) ? def : n;
};

export const loadConfig = (): Oid4vcConfig => ({
  port: num(process.env.PORT, 3400),
  publicUrl: process.env.PUBLIC_URL || 'http://localhost:3400',
  credentialServiceBaseUrl:
    process.env.CREDENTIAL_SERVICE_BASE_URL || 'http://localhost:3000',
  identityBaseUrl: process.env.IDENTITY_BASE_URL || 'http://localhost:3332',
  schemaBaseUrl: process.env.SCHEMA_BASE_URL || 'http://localhost:3333',
  sessionStore: process.env.SESSION_STORE === 'redis' ? 'redis' : 'memory',
  redisUrl: process.env.REDIS_URL || 'redis://localhost:6379',
  issuerDid: process.env.ISSUER_DID || '',
  oid4vpEnabled: process.env.OID4VP_ENABLED !== 'false',
  draft13CompatMode: process.env.DRAFT13_COMPAT_MODE === 'true',
  // OID4VP request-object mode. Legacy implies unsigned (the `redirect_uri`
  // client_id scheme MUST NOT be used with a signed request object), so it
  // overrides vpSignRequest regardless of how that flag is set.
  vpSignRequest:
    process.env.OID4VP_LEGACY_CLIENT_ID_SCHEME !== 'true' &&
    process.env.OID4VP_SIGN_REQUEST !== 'false',
  vpLegacyClientIdScheme: process.env.OID4VP_LEGACY_CLIENT_ID_SCHEME === 'true',
  // Deliberately NOT falling back to ISSUER_DID here. ISSUER_DID is commonly a
  // did:rcw (identity-service's own method — the documented setup step for it
  // uses method "rcw"), which only identity-service can resolve, so no wallet
  // could verify a request object signed with it. An explicit VERIFIER_DID is
  // an operator's deliberate choice and is trusted as-is; the ISSUER_DID /
  // auto-provisioned fallbacks are resolvability-checked in oid4vp.service.ts.
  verifierDid: process.env.VERIFIER_DID || '',
  ttl: {
    offer: num(process.env.OFFER_TTL, 600),
    nonce: num(process.env.NONCE_TTL, 300),
    accessToken: num(process.env.ACCESS_TOKEN_TTL, 300),
    deferred: num(process.env.DEFERRED_TTL, 86400),
    vpTxn: num(process.env.VP_TXN_TTL, 300),
  },
});

export const CONFIG = Symbol('OID4VC_CONFIG');
