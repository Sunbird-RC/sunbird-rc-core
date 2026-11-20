import { statusListTokenUri } from './status-list-uri';

describe('statusListTokenUri', () => {
  const listId = 'did:rcw:list-1';
  const ENV_KEYS = [
    'CREDENTIAL_SERVICE_PUBLIC_URL',
    'CREDENTIAL_SERVICE_BASE_URL',
  ] as const;
  const saved: Record<string, string | undefined> = {};

  beforeEach(() => {
    for (const key of ENV_KEYS) {
      saved[key] = process.env[key];
      delete process.env[key];
    }
  });

  afterEach(() => {
    for (const key of ENV_KEYS) {
      if (saved[key] === undefined) delete process.env[key];
      else process.env[key] = saved[key];
    }
  });

  it('builds the token URL from CREDENTIAL_SERVICE_PUBLIC_URL when set', () => {
    process.env.CREDENTIAL_SERVICE_PUBLIC_URL =
      'https://credentials.example.com';
    // The internal Docker address must not leak into the claim's uri.
    process.env.CREDENTIAL_SERVICE_BASE_URL = 'http://credential:3000';

    expect(statusListTokenUri(listId)).toBe(
      'https://credentials.example.com/credentials/status-list/did:rcw:list-1/token'
    );
  });

  it('falls back to CREDENTIAL_SERVICE_BASE_URL when the public URL is unset', () => {
    process.env.CREDENTIAL_SERVICE_BASE_URL =
      'https://credentials.example.com';

    expect(statusListTokenUri(listId)).toBe(
      'https://credentials.example.com/credentials/status-list/did:rcw:list-1/token'
    );
  });

  it('falls back to CREDENTIAL_SERVICE_BASE_URL when the public URL is empty', () => {
    process.env.CREDENTIAL_SERVICE_PUBLIC_URL = '';
    process.env.CREDENTIAL_SERVICE_BASE_URL = 'http://credential:3000';

    expect(statusListTokenUri(listId)).toBe(
      'http://credential:3000/credentials/status-list/did:rcw:list-1/token'
    );
  });
});
