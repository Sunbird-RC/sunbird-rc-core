import {
  VCValidator,
  UnsignedVCValidator,
  AddressValidator,
  IdentifierTypeValidator,
  CredentialSubjectValidator,
  ImageValidator,
  CredentialStatusValidator,
  CredentialSchemaValidator,
  ProofValidator,
} from './index';

describe('VCValidator', () => {
  const validVC = {
    '@context': ['https://www.w3.org/2018/credentials/v1'],
    id: 'urn:uuid:123',
    type: ['VerifiableCredential'],
    issuer: 'did:rcw:issuer-1',
    issuanceDate: '2024-01-01T00:00:00Z',
    credentialSubject: { id: 'did:rcw:subject-1' },
    proof: {
      type: 'Ed25519Signature2020',
      created: '2024-01-01T00:00:00Z',
      proofPurpose: 'assertionMethod',
      verificationMethod: 'did:rcw:issuer-1#key-1',
      proofValue: 'z123',
    },
  };

  it('accepts a valid VC', () => {
    expect(() => VCValidator.parse(validVC)).not.toThrow();
  });

  it('rejects VC missing @context', () => {
    const { '@context': _, ...rest } = validVC;
    expect(() => VCValidator.parse(rest)).toThrow();
  });

  it('rejects VC missing issuer', () => {
    const { issuer: _, ...rest } = validVC;
    expect(() => VCValidator.parse(rest)).toThrow();
  });

  it('rejects VC missing proof', () => {
    const { proof: _, ...rest } = validVC;
    expect(() => VCValidator.parse(rest)).toThrow();
  });

  it('rejects VC missing credentialSubject', () => {
    const { credentialSubject: _, ...rest } = validVC;
    expect(() => VCValidator.parse(rest)).toThrow();
  });
});

describe('UnsignedVCValidator', () => {
  const validUnsigned = {
    '@context': ['https://www.w3.org/2018/credentials/v1'],
    id: 'urn:uuid:123',
    type: ['VerifiableCredential'],
    issuer: 'did:rcw:issuer-1',
    issuanceDate: '2024-01-01T00:00:00Z',
    credentialSubject: { id: 'did:rcw:subject-1' },
  };

  it('accepts a valid unsigned VC', () => {
    expect(() => UnsignedVCValidator.parse(validUnsigned)).not.toThrow();
  });

  it('rejects VC with empty type array (nonempty required)', () => {
    expect(() => UnsignedVCValidator.parse({ ...validUnsigned, type: [] })).toThrow();
  });

  it('accepts VC with issuer as string', () => {
    expect(() => UnsignedVCValidator.parse({ ...validUnsigned, issuer: 'did:rcw:123' })).not.toThrow();
  });

  it('accepts VC with issuer as object', () => {
    expect(() => UnsignedVCValidator.parse({ ...validUnsigned, issuer: { id: 'did:rcw:123' } })).not.toThrow();
  });

  it('rejects VC missing type', () => {
    const { type: _, ...rest } = validUnsigned;
    expect(() => UnsignedVCValidator.parse(rest)).toThrow();
  });
});

describe('ImageValidator', () => {
  it('accepts a string URL', () => {
    expect(() => ImageValidator.parse('https://example.com/image.png')).not.toThrow();
  });

  it('accepts an object with id and type', () => {
    expect(() => ImageValidator.parse({ id: 'https://example.com/img.png', type: 'Image' })).not.toThrow();
  });

  it('accepts an object with optional caption', () => {
    expect(() => ImageValidator.parse({ id: 'https://example.com/img.png', type: 'Image', caption: 'A photo' })).not.toThrow();
  });

  it('rejects object missing id', () => {
    expect(() => ImageValidator.parse({ type: 'Image' })).toThrow();
  });
});

describe('AddressValidator', () => {
  const validAddress = {
    type: 'PostalAddress',
    streetAddress: '123 Main St',
    addressLocality: 'Springfield',
    addressRegion: 'IL',
    postalCode: '62701',
    addressCountry: 'US',
  };

  it('accepts a valid address', () => {
    expect(() => AddressValidator.parse(validAddress)).not.toThrow();
  });

  it('accepts address with geo', () => {
    expect(() => AddressValidator.parse({
      ...validAddress,
      geo: { type: 'GeoCoordinates', latitude: 39.78, longitude: -89.65 },
    })).not.toThrow();
  });

  it('accepts address without optional fields', () => {
    expect(() => AddressValidator.parse({ type: 'PostalAddress' })).not.toThrow();
  });
});

describe('IdentifierTypeValidator', () => {
  it('accepts known OpenBadges identifier types', () => {
    expect(() => IdentifierTypeValidator.parse('email')).not.toThrow();
    expect(() => IdentifierTypeValidator.parse('phone')).not.toThrow();
    expect(() => IdentifierTypeValidator.parse('url')).not.toThrow();
    expect(() => IdentifierTypeValidator.parse('twitter')).not.toThrow();
    expect(() => IdentifierTypeValidator.parse('github')).not.toThrow();
  });

  it('accepts arbitrary string as fallback', () => {
    expect(() => IdentifierTypeValidator.parse('custom-type')).not.toThrow();
  });
});

describe('CredentialSubjectValidator', () => {
  it('accepts an object with id', () => {
    expect(() => CredentialSubjectValidator.parse({ id: 'did:rcw:123' })).not.toThrow();
  });

  it('accepts an object without id', () => {
    expect(() => CredentialSubjectValidator.parse({ name: 'test' })).not.toThrow();
  });

  it('accepts arbitrary fields via catchall', () => {
    expect(() => CredentialSubjectValidator.parse({ grade: 'A', programme: 'CS' })).not.toThrow();
  });
});

describe('CredentialStatusValidator', () => {
  it('accepts valid credential status', () => {
    expect(() => CredentialStatusValidator.parse({
      type: 'RevocationList2020Status',
      id: 'https://example.com/status#42',
    })).not.toThrow();
  });

  it('rejects missing type', () => {
    expect(() => CredentialStatusValidator.parse({ id: 'https://example.com/status#42' })).toThrow();
  });
});

describe('CredentialSchemaValidator', () => {
  it('accepts valid schema reference', () => {
    expect(() => CredentialSchemaValidator.parse({
      id: 'https://example.com/schema/1',
      type: 'JsonSchemaValidator2018',
    })).not.toThrow();
  });

  it('rejects missing id', () => {
    expect(() => CredentialSchemaValidator.parse({ type: 'JsonSchemaValidator2018' })).toThrow();
  });
});

describe('ProofValidator', () => {
  const validProof = {
    type: 'Ed25519Signature2020',
    created: '2024-01-01T00:00:00Z',
    proofPurpose: 'assertionMethod',
    verificationMethod: 'did:rcw:issuer-1#key-1',
    proofValue: 'z123',
  };

  it('accepts a valid proof', () => {
    expect(() => ProofValidator.parse(validProof)).not.toThrow();
  });

  it('rejects proof missing type', () => {
    const { type: _, ...rest } = validProof;
    expect(() => ProofValidator.parse(rest)).toThrow();
  });

  it('accepts proof without proofValue (catchall allows it)', () => {
    const { proofValue: _, ...rest } = validProof;
    expect(() => ProofValidator.parse(rest)).not.toThrow();
  });
});
