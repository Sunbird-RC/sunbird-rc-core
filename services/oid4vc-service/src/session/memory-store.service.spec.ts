import { MemoryStoreService } from './memory-store.service';

describe('MemoryStoreService', () => {
  let store: MemoryStoreService;

  beforeEach(() => {
    store = new MemoryStoreService();
  });

  it('stores and reads a value', async () => {
    await store.set('k', { a: 1 }, 60);
    expect(await store.get('k')).toEqual({ a: 1 });
  });

  it('getdel returns the value once, then null (single-use / replay-safe)', async () => {
    await store.set('nonce:x', '1', 60);
    expect(await store.getdel('nonce:x')).toBe('1');
    // second read must be null — this is what prevents nonce/code replay
    expect(await store.getdel('nonce:x')).toBeNull();
    expect(await store.get('nonce:x')).toBeNull();
  });

  it('honours TTL expiry', async () => {
    await store.set('short', 'v', -1); // already expired
    expect(await store.get('short')).toBeNull();
  });

  it('del removes a key', async () => {
    await store.set('k', 'v', 60);
    await store.del('k');
    expect(await store.get('k')).toBeNull();
  });
});
