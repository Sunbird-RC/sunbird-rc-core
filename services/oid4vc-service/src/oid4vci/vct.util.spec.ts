import { isAbsoluteHttpUri, slugifyVct, normalizeVct } from './vct.util';

describe('vct.util', () => {
  describe('isAbsoluteHttpUri', () => {
    it('recognizes http(s) URIs', () => {
      expect(isAbsoluteHttpUri('https://issuer.example/vct/foo')).toBe(true);
      expect(isAbsoluteHttpUri('http://issuer.example/vct/foo')).toBe(true);
    });

    it('rejects bare names, even ones containing a colon', () => {
      expect(isAbsoluteHttpUri('National Identity Credential')).toBe(false);
      expect(isAbsoluteHttpUri('urn:example:foo')).toBe(false);
      expect(isAbsoluteHttpUri('did:web:issuer.example')).toBe(false);
    });
  });

  describe('slugifyVct', () => {
    it('lowercases and hyphenates a display name', () => {
      expect(slugifyVct('National Identity Credential')).toBe('national-identity-credential');
    });

    it('collapses punctuation and trims leading/trailing hyphens', () => {
      expect(slugifyVct('  Age Verification (New)!! ')).toBe('age-verification-new');
    });
  });

  describe('normalizeVct', () => {
    const publicUrl = 'https://verifier.example';

    it('passes an already-absolute URI through unchanged', () => {
      expect(normalizeVct('https://issuer.example/types/foo', publicUrl)).toBe(
        'https://issuer.example/types/foo',
      );
    });

    // The bug this whole module exists to fix: found live against walt.id's
    // wallet, which builds `http://localhost/.well-known/vct` + the raw vct
    // and crashes on the embedded space ("Illegal character in path at index
    // 40") because it resolves every vct as a URL regardless of RFC 7519's
    // StringOrURI carve-out for colon-free strings.
    it('turns a bare display name into a dereferenceable URI under publicUrl', () => {
      expect(normalizeVct('National Identity Credential', publicUrl)).toBe(
        'https://verifier.example/vct/national-identity-credential',
      );
    });

    it('strips a trailing slash on publicUrl before appending the path', () => {
      expect(normalizeVct('National Identity Credential', 'https://verifier.example/')).toBe(
        'https://verifier.example/vct/national-identity-credential',
      );
    });

    it('passes through unchanged if nothing is sluggable (defensive fallback)', () => {
      expect(normalizeVct('!!!', publicUrl)).toBe('!!!');
    });

    it('passes through a falsy vct unchanged', () => {
      expect(normalizeVct('', publicUrl)).toBe('');
    });
  });
});
