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
    return {
      id: 'did:example:123456789abcdefghi',
    };
  }
}
