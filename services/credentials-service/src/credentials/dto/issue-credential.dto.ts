import { W3CCredential } from 'vc.types';

export class IssueCredentialDTO {
  credential: W3CCredential;
  credentialSchemaId: string; // DID of the schema
  credentialSchemaVersion: string;
  tags: string[];
  method?: string;
}
