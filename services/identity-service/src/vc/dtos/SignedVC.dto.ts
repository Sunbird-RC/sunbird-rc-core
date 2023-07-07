import { ApiProperty } from "@nestjs/swagger";

export class SignedVC {

    @ApiProperty({ description: 'Public Key of issuer.' })
    publicKey: JsonWebKey;

    @ApiProperty({ description: 'Signed VC' })
    signed: any
}