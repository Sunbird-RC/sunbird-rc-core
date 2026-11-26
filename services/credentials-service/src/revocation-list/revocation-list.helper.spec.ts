import { inflateSync } from 'zlib';
import { RevocationList } from './revocation-list.helper';

describe('RevocationList', () => {
  it('creates a list with default length 100000', () => {
    const list = new RevocationList();
    expect(list.isRevoked(0)).toBe(false);
  });

  it('creates a list with custom length', () => {
    const list = new RevocationList({ length: 100 });
    expect(list.isRevoked(0)).toBe(false);
  });

  describe('setRevoked / isRevoked', () => {
    it('marks an index as revoked and reads it back', () => {
      const list = new RevocationList({ length: 1000 });
      list.setRevoked(42, true);
      expect(list.isRevoked(42)).toBe(true);
    });

    it('marks an index as not revoked', () => {
      const list = new RevocationList({ length: 1000 });
      list.setRevoked(42, true);
      list.setRevoked(42, false);
      expect(list.isRevoked(42)).toBe(false);
    });

    it('does not affect other indices', () => {
      const list = new RevocationList({ length: 1000 });
      list.setRevoked(10, true);
      expect(list.isRevoked(11)).toBe(false);
      expect(list.isRevoked(9)).toBe(false);
    });

    it('throws TypeError when revoked is not a boolean', () => {
      const list = new RevocationList({ length: 1000 });
      expect(() => list.setRevoked(0, 'true' as any)).toThrow(TypeError);
      expect(() => list.setRevoked(0, 1 as any)).toThrow(TypeError);
      expect(() => list.setRevoked(0, null as any)).toThrow(TypeError);
    });

    it('handles multiple revocations independently', () => {
      const list = new RevocationList({ length: 10000 });
      list.setRevoked(0, true);
      list.setRevoked(9999, true);
      list.setRevoked(5000, true);

      expect(list.isRevoked(0)).toBe(true);
      expect(list.isRevoked(9999)).toBe(true);
      expect(list.isRevoked(5000)).toBe(true);
      expect(list.isRevoked(1)).toBe(false);
      expect(list.isRevoked(4999)).toBe(false);
    });
  });

  describe('encode / decode round-trip', () => {
    it('round-trips revoked and unrevoked indices through encode/decode', async () => {
      const list = new RevocationList({ length: 1000 });
      list.setRevoked(10, true);
      list.setRevoked(500, true);

      const encoded = await list.encode();
      const decoded = await RevocationList.decode({ encodedList: encoded });

      expect(decoded.isRevoked(10)).toBe(true);
      expect(decoded.isRevoked(500)).toBe(true);
      expect(decoded.isRevoked(0)).toBe(false);
      expect(decoded.isRevoked(11)).toBe(false);
      expect(decoded.isRevoked(999)).toBe(false);
    });
  });
});

// The IETF Token Status List encoding (draft-ietf-oauth-status-list) reads
// bit `idx` from byte `idx >> 3`, bit `idx & 7`, counting from the LEAST
// significant bit — the opposite order from the RevocationList2020
// bitstring this class wraps. These tests pin the conversion down.
const ietfBitAt = (lst: string, idx: number): number => {
  const bytes = inflateSync(Buffer.from(lst, 'base64url'));
  return (bytes[idx >> 3] >> (idx & 7)) & 1;
};

describe('RevocationList — encodeStatusList (IETF Token Status List)', () => {
  it('sets exactly the revoked indices, LSB-first', () => {
    const list = new RevocationList({ length: 32 });
    list.setRevoked(0, true);
    list.setRevoked(5, true);
    list.setRevoked(9, true);
    list.setRevoked(31, true);

    const lst = list.encodeStatusList();

    for (const idx of [0, 5, 9, 31]) {
      expect(ietfBitAt(lst, idx)).toBe(1);
    }
    for (const idx of [1, 4, 6, 8, 10, 30]) {
      expect(ietfBitAt(lst, idx)).toBe(0);
    }
  });

  it('encodes an untouched list as all zeroes', () => {
    const list = new RevocationList({ length: 16 });
    const bytes = inflateSync(Buffer.from(list.encodeStatusList(), 'base64url'));
    expect(Array.from(bytes)).toEqual([0, 0]);
  });

  it('agrees with isRevoked at every index after a W3C encode/decode round-trip', async () => {
    const list = new RevocationList({ length: 64 });
    for (const idx of [3, 7, 8, 15, 16, 63]) {
      list.setRevoked(idx, true);
    }
    const decoded = await RevocationList.decode({
      encodedList: await list.encode(),
    });

    const lst = decoded.encodeStatusList();

    for (let idx = 0; idx < 64; idx++) {
      expect(ietfBitAt(lst, idx)).toBe(decoded.isRevoked(idx) ? 1 : 0);
    }
  });

  it('is not the W3C byte order: bit 0 lands on a different position', async () => {
    const list = new RevocationList({ length: 16 });
    list.setRevoked(0, true);

    // W3C RevocationList2020: MSB-first, so bit 0 is 0x80 in byte 0.
    const decoded = (await RevocationList.decode({
      encodedList: await list.encode(),
    })) as any;
    expect(decoded.bitstring.bits[0]).toBe(0x80);

    // IETF: LSB-first, so bit 0 is 0x01 in byte 0.
    const ietfBytes = inflateSync(
      Buffer.from(list.encodeStatusList(), 'base64url')
    );
    expect(ietfBytes[0]).toBe(0x01);
  });
});
