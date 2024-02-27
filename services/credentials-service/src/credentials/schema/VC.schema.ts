export class IssuedVerifiableCredential {
  '@context': ReadonlyArray<string>;
  id: string;
  type: ReadonlyArray<string>;
  issuer: {
    id: string;
  };
  issuanceDate: string;
  expirationDate: string;
  credentialSubject: JSON;
  proof: {
    type: string;
    created: string;
    challenge: string;
    domain: string;
    nonce: string;
    verificationMethod: string;
    proofPurpose: string;
    jws: string;
    proofValue: string;
  };
}
