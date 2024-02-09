import { ApiProperty } from '@nestjs/swagger';

export class VerifyJsonDTO {
  @ApiProperty()
  DID: string;
  payload: string; //Only string is supported for now
}
