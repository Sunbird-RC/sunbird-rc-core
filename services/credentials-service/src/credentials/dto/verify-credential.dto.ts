import { IssuedVerifiableCredential } from '../schema/VC.schema';

export class VerifyCredentialDTO {
  verifiableCredential: IssuedVerifiableCredential | Record<string, any>;
  options: {
    challenge: string;
    domain: string;
  };
}
