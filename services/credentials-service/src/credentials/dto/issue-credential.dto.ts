import { W3CCredential } from 'vc.types';

// Supported wire formats for an issued credential.
// Absent format = 'ldp_vc' = today's behaviour, byte-for-byte.
export type CredentialFormat = 'ldp_vc' | 'jwt_vc_json' | 'vc+sd-jwt';

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
  // vc+sd-jwt only: holder's public JWK for key binding (cnf claim).
  holderJwk?: Record<string, any>;
}
