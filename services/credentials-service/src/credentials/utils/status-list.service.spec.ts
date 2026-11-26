import { inflateSync } from 'zlib';
import { RevocationListImpl } from '../../revocation-list/revocation-list.impl';
import { StatusListService } from './status-list.service';

// allocateIndex() tests use a real-ish CAS loop with mocked prisma — unchanged.
describe('StatusListService — allocateIndex', () => {
  const issuer = 'did:rcw:issuer-1';

  const makeService = (prisma: any) =>
    new StatusListService(prisma, { createList: jest.fn() } as any, {} as any);

  it('allocates the current index and bumps the counter by one', async () => {
    const findUnique = jest.fn().mockResolvedValue({
      issuer,
      lastCredentialIdx: 5,
      latestRevocationListId: 'list-1',
    });
    const updateMany = jest.fn().mockResolvedValue({ count: 1 });
    const service = makeService({ revocationLists: { findUnique, updateMany } });

    const result = await service.allocateIndex(issuer);

    expect(result).toEqual({ statusListCredential: 'list-1', index: 5 });
    expect(updateMany).toHaveBeenCalledWith({
      where: { issuer, lastCredentialIdx: 5 },
      data: { lastCredentialIdx: 6 },
    });
  });

  it('retries when a concurrent allocator already advanced the counter (CAS miss)', async () => {
    const findUnique = jest
      .fn()
      .mockResolvedValueOnce({ issuer, lastCredentialIdx: 5, latestRevocationListId: 'list-1' })
      .mockResolvedValueOnce({ issuer, lastCredentialIdx: 6, latestRevocationListId: 'list-1' });
    const updateMany = jest
      .fn()
      .mockResolvedValueOnce({ count: 0 })
      .mockResolvedValueOnce({ count: 1 });
    const service = makeService({ revocationLists: { findUnique, updateMany } });

    const result = await service.allocateIndex(issuer);

    expect(result).toEqual({ statusListCredential: 'list-1', index: 6 });
    expect(updateMany).toHaveBeenCalledTimes(2);
  });

  it('never hands out index 0 twice across a rollover', async () => {
    const findUnique = jest
      .fn()
      .mockResolvedValueOnce({ issuer, lastCredentialIdx: 99999, latestRevocationListId: 'list-1' })
      .mockResolvedValueOnce({ issuer, lastCredentialIdx: 0, latestRevocationListId: 'list-2' })
      .mockResolvedValueOnce({ issuer, lastCredentialIdx: 1, latestRevocationListId: 'list-2' });
    const updateMany = jest.fn().mockResolvedValue({ count: 1 });
    const createNewList = jest.fn().mockResolvedValue(undefined);
    const service = makeService({ revocationLists: { findUnique, updateMany } });
    (service as any).createNewList = createNewList;

    const first = await service.allocateIndex(issuer);
    expect(first).toEqual({ statusListCredential: 'list-2', index: 0 });
    expect(createNewList).toHaveBeenCalledTimes(1);

    const second = await service.allocateIndex(issuer);
    expect(second).toEqual({ statusListCredential: 'list-2', index: 1 });
    expect(updateMany).toHaveBeenCalledWith({
      where: { issuer, lastCredentialIdx: 0 },
      data: { lastCredentialIdx: 1 },
    });
  });

  it('creates a list on first-ever allocation for an issuer', async () => {
    const findUnique = jest
      .fn()
      .mockResolvedValueOnce(undefined)
      .mockResolvedValueOnce({ issuer, lastCredentialIdx: 0, latestRevocationListId: 'list-1' });
    const updateMany = jest.fn().mockResolvedValue({ count: 1 });
    const createNewList = jest.fn().mockResolvedValue(undefined);
    const service = makeService({ revocationLists: { findUnique, updateMany } });
    (service as any).createNewList = createNewList;

    const result = await service.allocateIndex(issuer);

    expect(result).toEqual({ statusListCredential: 'list-1', index: 0 });
    expect(createNewList).toHaveBeenCalledWith(issuer);
  });
});

describe('StatusListService — buildCredentialStatus', () => {
  it('returns correctly shaped credentialStatus object', () => {
    const service = new StatusListService({} as any, {} as any, {} as any);
    const result = service.buildCredentialStatus('list-cred-id', 42);

    expect(result).toEqual({
      id: 'list-cred-id#42',
      type: 'RevocationList2020Status',
      revocationListIndex: '42',
      revocationListCredential: 'list-cred-id',
    });
  });

  it('handles index 0', () => {
    const service = new StatusListService({} as any, {} as any, {} as any);
    const result = service.buildCredentialStatus('list-1', 0);
    expect(result.revocationListIndex).toBe('0');
    expect(result.id).toBe('list-1#0');
  });
});

describe('StatusListService — isRevoked', () => {
  const makeService = (prisma: any, rl: any) =>
    new StatusListService(prisma, rl, {} as any);

  it('returns true when the bit is set', async () => {
    const mockList = { isRevoked: jest.fn().mockReturnValue(true) };
    const rl = { decodeList: jest.fn().mockResolvedValue(mockList) };
    const prisma = {
      verifiableCredentials: {
        findUnique: jest.fn().mockResolvedValue({
          subject: { encodedList: 'encoded-data' },
        }),
      },
    };
    const service = makeService(prisma, rl);

    const result = await service.isRevoked('list-1', 5);
    expect(result).toBe(true);
    expect(mockList.isRevoked).toHaveBeenCalledWith(5);
  });

  it('returns false when the bit is not set', async () => {
    const mockList = { isRevoked: jest.fn().mockReturnValue(false) };
    const rl = { decodeList: jest.fn().mockResolvedValue(mockList) };
    const prisma = {
      verifiableCredentials: {
        findUnique: jest.fn().mockResolvedValue({
          subject: { encodedList: 'encoded-data' },
        }),
      },
    };
    const service = makeService(prisma, rl);

    expect(await service.isRevoked('list-1', 5)).toBe(false);
  });

  it('returns false when list VC not found', async () => {
    const prisma = {
      verifiableCredentials: { findUnique: jest.fn().mockResolvedValue(null) },
    };
    const service = makeService(prisma, {} as any);
    expect(await service.isRevoked('nonexistent', 0)).toBe(false);
  });

  it('returns false when encodedList missing', async () => {
    const prisma = {
      verifiableCredentials: {
        findUnique: jest.fn().mockResolvedValue({ subject: {} }),
      },
    };
    const service = makeService(prisma, {} as any);
    expect(await service.isRevoked('list-1', 0)).toBe(false);
  });

  it('returns false on decode error', async () => {
    const rl = { decodeList: jest.fn().mockRejectedValue(new Error('bad')) };
    const prisma = {
      verifiableCredentials: {
        findUnique: jest.fn().mockResolvedValue({
          subject: { encodedList: 'bad' },
        }),
      },
    };
    const service = makeService(prisma, rl);
    expect(await service.isRevoked('list-1', 0)).toBe(false);
  });
});

describe('StatusListService — setRevoked', () => {
  it('flips the bit, re-signs, and updates the DB', async () => {
    const mockList = {
      isRevoked: jest.fn().mockReturnValue(false),
      setRevoked: jest.fn(),
      encode: jest.fn().mockResolvedValue('updated-encoded'),
    };
    const rl = { decodeList: jest.fn().mockResolvedValue(mockList) };
    const signVC = jest.fn().mockResolvedValue({ proof: { type: 'Ed25519Signature2020', proofValue: 'sig' } });
    const identityService = { signVC } as any;
    const update = jest.fn().mockResolvedValue({});
    const prisma = {
      verifiableCredentials: {
        findUnique: jest.fn().mockResolvedValue({
          id: 'list-1',
          subject: { encodedList: 'old-encoded', id: 'list-1', type: 'RevocationList2020' },
          signed: { id: 'list-1' },
        }),
        update,
      },
    };
    const service = new StatusListService(prisma as any, rl as any, identityService);

    await service.setRevoked('list-1', 5, 'did:rcw:issuer');

    expect(mockList.setRevoked).toHaveBeenCalledWith(5, true);
    expect(mockList.encode).toHaveBeenCalled();
    expect(signVC).toHaveBeenCalled();
    expect(update).toHaveBeenCalledWith(
      expect.objectContaining({
        where: { id: 'list-1' },
        data: expect.objectContaining({ subject: expect.any(Object), proof: expect.any(Object) }),
      })
    );
  });

  it('throws when list VC not found', async () => {
    const prisma = {
      verifiableCredentials: { findUnique: jest.fn().mockResolvedValue(null) },
    };
    const service = new StatusListService(prisma as any, {} as any, {} as any);
    await expect(service.setRevoked('nonexistent', 0, 'did:rcw:issuer')).rejects.toThrow();
  });
});

describe('StatusListService — getStatusListCredential', () => {
  it('returns signed credential when found', async () => {
    const prisma = {
      verifiableCredentials: {
        findUnique: jest.fn().mockResolvedValue({ signed: { id: 'list-1', proof: {} } }),
      },
    };
    const service = new StatusListService(prisma as any, {} as any, {} as any);
    const result = await service.getStatusListCredential('list-1');
    expect(result).toEqual({ id: 'list-1', proof: {} });
  });

  it('returns null when not found', async () => {
    const prisma = {
      verifiableCredentials: { findUnique: jest.fn().mockResolvedValue(null) },
    };
    const service = new StatusListService(prisma as any, {} as any, {} as any);
    expect(await service.getStatusListCredential('nonexistent')).toBeNull();
  });
});

describe('StatusListService — getStatusListToken (issue → revoke → fetch token)', () => {
  const issuer = 'did:rcw:issuer-1';
  const listId = 'did:rcw:list-1';

  // IETF reading of a status list: bit `idx` is byte `idx >> 3`, bit
  // `idx & 7`, LSB-first, of the DEFLATE-inflated `lst`.
  const ietfBitAt = (lst: string, idx: number): number => {
    const bytes = inflateSync(Buffer.from(lst, 'base64url'));
    return (bytes[idx >> 3] >> (idx & 7)) & 1;
  };
  const payloadOf = (token: string) =>
    JSON.parse(Buffer.from(token.split('.')[1], 'base64url').toString());

  const makeService = async () => {
    const rl = new RevocationListImpl();
    // The list row as persisted when the list was created: bitstring with
    // nothing revoked yet.
    const fresh = rl.createList({ length: 16 });
    const row: any = {
      id: listId,
      issuer,
      subject: {
        id: listId,
        type: 'RevocationList2020',
        encodedList: await fresh.encode(),
      },
      signed: { id: listId },
    };
    const prisma: any = {
      revocationLists: {
        findUnique: jest.fn().mockResolvedValue({
          issuer,
          lastCredentialIdx: 5,
          latestRevocationListId: listId,
        }),
        updateMany: jest.fn().mockResolvedValue({ count: 1 }),
      },
      verifiableCredentials: {
        findUnique: jest.fn(async ({ where }: any) =>
          where.id === listId ? row : null
        ),
        update: jest.fn(async ({ data }: any) => {
          row.subject = data.subject;
          row.proof = data.proof;
          row.signed = data.signed;
          return row;
        }),
      },
    };
    const signJwt = jest.fn(
      async (_did: any, payload: any) =>
        `header.${Buffer.from(JSON.stringify(payload)).toString('base64url')}.signature`
    );
    const identity: any = {
      signJwt,
      signVC: jest
        .fn()
        .mockResolvedValue({
          proof: { type: 'Ed25519Signature2020', proofValue: 'zProof' },
        }),
    };
    const service = new StatusListService(prisma, rl, identity);
    return { service, signJwt };
  };

  beforeAll(() => {
    process.env.CREDENTIAL_SERVICE_BASE_URL = 'https://credentials.example.com';
  });
  afterAll(() => {
    delete process.env.CREDENTIAL_SERVICE_BASE_URL;
  });

  it('serves a token whose bit idx is set only after the credential is revoked', async () => {
    const { service, signJwt } = await makeService();

    // Issue: allocate the index and build the credential's status entry.
    const allocation = await service.allocateIndex(issuer);
    expect(allocation).toEqual({ statusListCredential: listId, index: 5 });
    const status = service.buildCredentialStatus(
      allocation.statusListCredential,
      allocation.index
    );
    expect(status.revocationListIndex).toBe('5');

    // Fetch the token before revocation: bit 5 is clear.
    const before = await service.getStatusListToken(listId);
    expect(before).not.toBeNull();
    expect(ietfBitAt(payloadOf(before.token).status_list.lst, 5)).toBe(0);

    // Revoke the credential.
    await service.setRevoked(listId, allocation.index, issuer);

    // Fetch again — the token is generated per request, so bit 5 is set.
    const after = await service.getStatusListToken(listId);
    const payload = payloadOf(after.token);
    expect(payload.status_list.bits).toBe(1);
    expect(ietfBitAt(payload.status_list.lst, 5)).toBe(1);
    expect(ietfBitAt(payload.status_list.lst, 4)).toBe(0);
    expect(ietfBitAt(payload.status_list.lst, 6)).toBe(0);

    // Token claims per the maintainer's spec.
    expect(payload.iss).toBe(issuer);
    expect(payload.sub).toBe(
      `https://credentials.example.com/credentials/status-list/${listId}/token`
    );
    expect(payload.ttl).toBe(300);
    expect(payload.exp - payload.iat).toBe(24 * 60 * 60);
    expect(after.ttl).toBe(300);
    expect(signJwt).toHaveBeenCalledWith(issuer, expect.any(Object), {
      typ: 'statuslist+jwt',
    });
  });

  it('uses CREDENTIAL_SERVICE_PUBLIC_URL for the token sub when set', async () => {
    process.env.CREDENTIAL_SERVICE_PUBLIC_URL =
      'https://public.example.com';
    try {
      const { service } = await makeService();
      const result = await service.getStatusListToken(listId);
      expect(result).not.toBeNull();
      // sub must match the status.status_list.uri in issued credentials:
      // the public URL, not the base URL configured for this suite.
      expect(payloadOf(result.token).sub).toBe(
        `https://public.example.com/credentials/status-list/${listId}/token`
      );
    } finally {
      delete process.env.CREDENTIAL_SERVICE_PUBLIC_URL;
    }
  });

  it('returns null for an unknown status list', async () => {
    const { service } = await makeService();
    expect(await service.getStatusListToken('did:rcw:unknown')).toBeNull();
  });
});
