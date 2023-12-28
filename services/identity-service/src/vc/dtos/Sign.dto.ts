import { IonDocumentModel } from '@decentralized-identity/ion-sdk';
import { ApiProperty } from '@nestjs/swagger';

export class SignJsonDTO {
  @ApiProperty({
    description: 'The unique DID id of the issuer.'
  })
  DID: string;

  @ApiProperty({
    description: 'JSON string to be signed.'
  })
  payload: string; //Only string is supported for now

  @ApiProperty({
    description: 'Boolean property to stat if the payload is a unsigned VC'
  })
  isVerifiableCredential?: boolean;
}
