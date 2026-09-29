import * as crypto from 'crypto';
import * as bs58 from 'bs58';
import { digestMultibase } from './multibase.util';

function expectedDigest(content: string | Buffer): string {
  const digest = crypto.createHash('sha256').update(content).digest();
  const multihash = Buffer.concat([Buffer.from([0x12, 0x20]), digest]);
  return `z${bs58.encode(multihash)}`;
}

describe('digestMultibase', () => {
  it('matches an independently computed multibase(sha256) digest', () => {
    expect(digestMultibase('hello')).toBe(expectedDigest('hello'));
  });

  it('is deterministic for the same input', () => {
    expect(digestMultibase('hello')).toBe(digestMultibase('hello'));
  });

  it('produces different output for different inputs', () => {
    expect(digestMultibase('hello')).not.toBe(digestMultibase('world'));
  });

  it('always starts with the "z" multibase prefix', () => {
    expect(digestMultibase('hello').startsWith('z')).toBe(true);
    expect(digestMultibase('').startsWith('z')).toBe(true);
  });

  it('produces the same digest for equivalent string and Buffer input', () => {
    expect(digestMultibase('hello')).toBe(digestMultibase(Buffer.from('hello')));
  });
});
