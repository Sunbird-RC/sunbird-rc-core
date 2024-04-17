import { ApiProperty } from '@nestjs/swagger';

export class SignJsonDTO {
  @ApiProperty({
    description: 'The unique DID id of the issuer.'
  })
  DID: string;

  @ApiProperty({
    description: 'JSON LD of the unsigned VC.'
  })
  payload: object; //Only JSON LD is supported for now
}
