import { Injectable } from '@nestjs/common';
import { RevocationList } from './revocation-list.helper';

@Injectable()
export class RevocationListImpl {
  constructor() {}

  public createList({ length }) {
    return new RevocationList({ length });
  }

  public async decodeList({ encodedList }) {
    return await RevocationList.decode({ encodedList });
  }
}
