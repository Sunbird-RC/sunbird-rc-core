import { MetadataController } from './metadata.controller';

describe('MetadataController', () => {
  let oid4vci: any;
  let tokens: any;
  let controller: MetadataController;

  beforeEach(() => {
    oid4vci = { issuerMetadata: jest.fn() };
    tokens = { asMetadata: jest.fn(), jwks: jest.fn() };
    controller = new MetadataController(oid4vci, tokens);
  });

  describe('issuerMetadata', () => {
    it('delegates to oid4vci.issuerMetadata', () => {
      const expected = { credential_issuer: 'https://issuer.example' };
      oid4vci.issuerMetadata.mockReturnValue(expected);

      const result = controller.issuerMetadata();
      expect(result).toBe(expected);
      expect(oid4vci.issuerMetadata).toHaveBeenCalled();
    });
  });

  describe('asMetadata', () => {
    it('delegates to tokens.asMetadata', () => {
      const expected = { issuer: 'https://issuer.example' };
      tokens.asMetadata.mockReturnValue(expected);

      const result = controller.asMetadata();
      expect(result).toBe(expected);
      expect(tokens.asMetadata).toHaveBeenCalled();
    });
  });

  describe('jwks', () => {
    it('delegates to tokens.jwks', () => {
      const expected = { keys: [] };
      tokens.jwks.mockReturnValue(expected);

      const result = controller.jwks();
      expect(result).toBe(expected);
      expect(tokens.jwks).toHaveBeenCalled();
    });
  });
});
