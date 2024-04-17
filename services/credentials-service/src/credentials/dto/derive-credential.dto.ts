import { IssuedVerifiableCredential } from '../schema/VC.schema';

export class DeriveCredentialDTO {
  verifiableCredential: IssuedVerifiableCredential;
  frame: JSON;
  options: {
    nonce: string;
  };
}
