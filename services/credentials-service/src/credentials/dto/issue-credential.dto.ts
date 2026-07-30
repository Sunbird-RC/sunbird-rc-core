import { W3CCredential } from 'vc.types';

// Supported wire formats for an issued credential.
// Absent format = 'ldp_vc' = today's behaviour, byte-for-byte.
export type CredentialFormat = 'ldp_vc' | 'jwt_vc_json' | 'vc+sd-jwt' | 'mso_mdoc';

export class IssueCredentialDTO {
  credential: W3CCredential;
  credentialSchemaId: string; // DID of the schema
  credentialSchemaVersion: string;
  tags: string[];
  method?: string;
  // OID4VC additions — all optional, additive:
  format?: CredentialFormat;
  // vc+sd-jwt only: top-level credentialSubject claims to make selectively
  // disclosable. Defaults to all subject claims except 'id'.
  disclosable?: string[];
  // vc+sd-jwt only: explicit `vct` (SD-JWT VC Type). Defaults to the last
  // entry of `credential.type` when omitted — see credential-format.service.ts
  // signSdJwtVc(). Callers that publish a normalized (URI-form) vct in their
  // own issuer metadata (e.g. oid4vc-service, see vct.util.ts) should pass the
  // exact same string here so the issued credential's vct matches what a
  // wallet resolved from metadata / DCQL vct_values.
  vct?: string;
  // vc+sd-jwt / mso_mdoc: holder's public JWK for key binding
  // (cnf claim / deviceKeyInfo.deviceKey respectively).
  holderJwk?: Record<string, any>;
  // vc+sd-jwt only: the holder's proof `kid` (a DID URL) when it bound via a
  // DID rather than an inline JWK. Bound as `cnf.kid` in preference to
  // `cnf.jwk`, which conformant wallets only accept for jwk-bound requests.
  holderKid?: string;
  // mso_mdoc only: has no W3C credentialSubject shape at all — claims are
  // organized under {namespace: {element: value}} instead. `credential` above
  // is still required (for its `id`/`type`/`issuer`), but its
  // `credentialSubject` is ignored for this format in favor of these fields.
  docType?: string;
  namespaces?: Record<string, Record<string, any>>;
}
