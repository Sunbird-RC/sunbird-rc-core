import { ApiProperty } from "@nestjs/swagger";
import { W3CCredential } from "did-jwt-vc";

export class GetCredentialByIdResponseDTO {
  @ApiProperty({
    description: 'Credential',
  })
  credential: W3CCredential;
}