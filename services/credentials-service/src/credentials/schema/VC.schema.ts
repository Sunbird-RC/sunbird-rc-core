export class IssuedVerifiableCredential {
  '@context': ReadonlyArray<string>;
  id: string;
  type: ReadonlyArray<string>;
  issuer: string | { id: string };
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
