import { StatusListService } from './status-list.service';

// allocateIndex() is a compare-and-swap loop over the mocked prisma client —
// these tests exercise the CAS/rollover logic directly without a real DB.
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
      // by the time we retry, the racer already bumped it to 6
      .mockResolvedValueOnce({ issuer, lastCredentialIdx: 6, latestRevocationListId: 'list-1' });
    const updateMany = jest
      .fn()
      .mockResolvedValueOnce({ count: 0 }) // lost the race on index 5
      .mockResolvedValueOnce({ count: 1 }); // won on index 6
    const service = makeService({ revocationLists: { findUnique, updateMany } });

    const result = await service.allocateIndex(issuer);

    expect(result).toEqual({ statusListCredential: 'list-1', index: 6 });
    expect(updateMany).toHaveBeenCalledTimes(2);
  });

  it('never hands out index 0 twice across a rollover', async () => {
    const findUnique = jest
      .fn()
      // first caller: list is full
      .mockResolvedValueOnce({ issuer, lastCredentialIdx: 99999, latestRevocationListId: 'list-1' })
      // after createNewList(), re-read sees the fresh list at index 0
      .mockResolvedValueOnce({ issuer, lastCredentialIdx: 0, latestRevocationListId: 'list-2' })
      // second caller (post-rollover) re-reads and must NOT see 0 again
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
    // the counter was bumped by allocateIndex's own CAS after rollover, not
    // left at 0 for the next caller to collide with.
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
