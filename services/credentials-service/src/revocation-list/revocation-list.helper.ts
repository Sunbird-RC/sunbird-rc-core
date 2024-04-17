// @ts-ignore
import { Bitstring } from '@techsavvyash/bitstring';

type BitstringConstructorParam = {
  length?: number,
  buffer?: Uint8Array
};

export class RevocationList {

  private bitstring: any;
  constructor({ length, buffer }: BitstringConstructorParam = { length: 100000 }) {
    this.bitstring = new Bitstring({ length, buffer });
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
    return this.bitstring.encodeBits();
  }

  static async decode({ encodedList }) {
    const buffer = await Bitstring.decodeBits({ encoded: encodedList });
    return new RevocationList({ buffer });
  }
}
