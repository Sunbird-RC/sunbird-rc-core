export class IssueCredentialDTO {
  credential: {
    '@context': string[];
    id: string;
    type: string[];
    issuer: string | { id: string };
    issuanceDate: string;
    expirationDate: string;
    credentialSubject: JSON;
    proof?: { [k: string]: any };
  };
  credentialSchemaId: string; // DID
  tags: string[];
}
