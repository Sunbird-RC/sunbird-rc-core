// @ts-ignore
import { Bitstring } from '@techsavvyash/bitstring';
import { gzipSync, gunzipSync, deflateSync } from 'zlib';

type BitstringConstructorParam = {
  length?: number;
  buffer?: Uint8Array;
};

export class RevocationList {
  private bitstring: any;
  constructor({ length, buffer }: BitstringConstructorParam = {}) {
    this.bitstring = buffer
      ? new Bitstring({ buffer })
      : new Bitstring({ length: length ?? 100000 });
  }

  setRevoked(index, revoked) {
    if (typeof revoked !== 'boolean') {
      throw new TypeError('revoked must be a boolean.');
    }

    return this.bitstring.set(index, revoked);
  }

  isRevoked(index) {
    return this.bitstring.get(index);
  }

  async encode() {
    return gzipSync(Buffer.from(this.bitstring.bits)).toString('base64url');
  }

  // IETF Token Status List encoding of the same bits
  // (draft-ietf-oauth-status-list, the `lst` value of a statuslist+jwt).
  // Two differences from the W3C encoding above, both required for Token
  // Status List verifiers to read it:
  //  - bit order is LSB-first within each byte (the RevocationList2020
  //    bitstring used here is MSB-first), and
  //  - compression is DEFLATE/zlib, not gzip.
  encodeStatusList() {
    const length = this.bitstring.length;
    const bytes = new Uint8Array(Math.ceil(length / 8));
    for (let i = 0; i < length; i++) {
      if (this.bitstring.get(i)) {
        bytes[i >> 3] |= 1 << (i & 7);
      }
    }
    return deflateSync(Buffer.from(bytes)).toString('base64url');
  }

  static async decode({ encodedList }) {
    const buffer = new Uint8Array(
      gunzipSync(Buffer.from(encodedList, 'base64url'))
    );
    return new RevocationList({ buffer });
  }
}
