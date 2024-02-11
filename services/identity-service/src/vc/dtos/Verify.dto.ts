import { ApiProperty } from '@nestjs/swagger';

export class VerifyJsonDTO {
  @ApiProperty()
  DID: string;
  payload: object; //Only object is supported for now
}
