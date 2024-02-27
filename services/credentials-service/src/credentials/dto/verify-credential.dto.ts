import { IssuedVerifiableCredential } from '../schema/VC.schema';

export class VerifyCredentialDTO {
  verifiableCredential: IssuedVerifiableCredential;
  options: {
    challenge: string;
    domain: string;
  };
}
