import { ApiProperty } from "@nestjs/swagger";
import { W3CCredential } from "vc.types";

export class GetCredentialByIdResponseDTO {
  @ApiProperty({
    description: 'Credential',
  })
  credential: W3CCredential;
}