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
  // vc+sd-jwt / mso_mdoc: holder's public JWK for key binding
  // (cnf claim / deviceKeyInfo.deviceKey respectively).
  holderJwk?: Record<string, any>;
  // mso_mdoc only: has no W3C credentialSubject shape at all — claims are
  // organized under {namespace: {element: value}} instead. `credential` above
  // is still required (for its `id`/`type`/`issuer`), but its
  // `credentialSubject` is ignored for this format in favor of these fields.
  docType?: string;
  namespaces?: Record<string, Record<string, any>>;
}
