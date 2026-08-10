import * as crypto from 'crypto';
import * as bs58 from 'bs58';

// multihash prefix for sha2-256: varint hash-function-code (0x12) + varint
// digest-length (0x20 = 32 bytes) — https://github.com/multiformats/multihash
const SHA2_256_MULTIHASH_PREFIX = Buffer.from([0x12, 0x20]);

// Computes a multibase (base58-btc, 'z' prefix) multihash digest of `content`
// — the format W3C VC Render Method's `digestMultibase` expects, matching the
// same multibase convention already used elsewhere in this ecosystem (e.g.
// did:key/did:web publicKeyMultibase values).
export function digestMultibase(content: string | Buffer): string {
  const digest = crypto
    .createHash('sha256')
    .update(content)
    .digest();
  const multihash = Buffer.concat([SHA2_256_MULTIHASH_PREFIX, digest]);
  return `z${bs58.encode(multihash)}`;
}
