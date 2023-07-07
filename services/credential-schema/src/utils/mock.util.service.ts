export class UtilsServiceMock {
  async sign(did: string, body: any) {
    const signedVCResponse = {
      data: {
        signed: 'mockSignedValue',
      },
    };

    const proof = {
      proofValue: signedVCResponse.data.signed as string,
      proofPurpose: 'assertionMethod',
      created: new Date().toISOString(),
      type: 'Ed25519Signature2020',
      verificationMethod: did,
    };
    return proof;
  }

  async generateDID(body: any) {
    const mockGenerateDID = [
      {
        '@context': 'https://w3id.org/did/v1',
        id: 'did:rcw:98bee106-9630-4a31-ba26-177c478431cc',
        alsoKnownAs: ['did.test@gmail.com.test'],
        service: [
          {
            id: 'IdentityHub',
            type: 'IdentityHub',
            serviceEndpoint: {
              '@context': 'schema.identity.foundation/hub',
              '@type': 'UserServiceEndpoint',
              instance: ['did:test:hub.id'],
            },
          },
        ],
        verificationMethod: [
          {
            id: 'auth-key',
            type: 'RS256',
            publicKeyJwk: {
              kty: 'EC',
              crv: 'secp256k1',
              x: 'u6YjEMn37er9zqqS4YTnyaXOuAgJ6hRD2z9tr3Yl5HI',
              y: 'j-BOp476lD9g_Ff0I8-wsENrDtOC4PvDQ0ssAFPTk4g',
            },
            controller: 'did:rcw:98bee106-9630-4a31-ba26-177c478431cc',
          },
        ],
        authentication: ['auth-key'],
      },
    ];
    return mockGenerateDID[0];
  }
}
