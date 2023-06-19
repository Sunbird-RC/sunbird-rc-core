import { ApiProperty } from "@nestjs/swagger";
import { W3CCredential } from "did-jwt-vc";
import { CredentialStatus, DateType, IssuerType } from "did-jwt-vc/lib/types";
import { Extensible } from "did-resolver";


// class Credential implements W3CCredential {
//   context: string | string[];
//   id?: string;
//   type: string[];
//   issuer: IssuerType;
//   issuanceDate: DateType;
//   expirationDate?: DateType;
//   credentialSubject: Extensible<{
//     id?: string
//   }>;
//   credentialStatus?: CredentialStatus
//   // eslint-disable-next-line @typescript-eslint/no-explicit-any
//   evidence?: any;
//   // eslint-disable-next-line @typescript-eslint/no-explicit-any
//   termsOfUse?: any;
// }
export class GetCredentialByIdResponseDTO {
  @ApiProperty({
    // type: Credential,
    description: 'Credential',
  })
  credential: W3CCredential;
}