import { ApiProperty } from "@nestjs/swagger";
import { W3CCredential } from "did-jwt-vc";

export class GetCredentialsByTagsResponseDTO {
  @ApiProperty({
    description: 'Array of credentials',
  })
  credentials: ReadonlyArray<W3CCredential>
}