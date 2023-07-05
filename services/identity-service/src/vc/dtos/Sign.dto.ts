import { IonDocumentModel } from '@decentralized-identity/ion-sdk';
import { ApiProperty } from '@nestjs/swagger';

export class SignJsonDTO {
  @ApiProperty({
    description: 'The unique DID id of the issuer.'
  })
  DID: string;

  @ApiProperty({
    description: 'JSON string of the unsigned VC.'
  })
  payload: string; //Only string is supported for now
}
