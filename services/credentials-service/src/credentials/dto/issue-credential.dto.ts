import { W3CCredential } from 'did-jwt-vc';

export class IssueCredentialDTO {
  credential: W3CCredential;
  credentialSchemaId: string; // DID of the schema
  tags: string[];
}
