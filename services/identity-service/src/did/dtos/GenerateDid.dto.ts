import { ApiProperty } from '@nestjs/swagger';
const { Service } = require('did-resolver');
type Service = typeof Service;

enum VerificationKeyType {
  Ed25519VerificationKey2020 = "Ed25519VerificationKey2020",
  Ed25519VerificationKey2018 = "Ed25519VerificationKey2018",
  RsaVerificationKey2018 = "RsaVerificationKey2018"
}

export class GenerateDidDTO {
  @ApiProperty({
    description:
      'AlsoKnownAs property is a unique combination aadhaar and username.',
    type: String,
    isArray: true,
  })
  alsoKnownAs?: string[];

  @ApiProperty({
    description:
      'An array of services that are used, for example a user registration service.',
    isArray: true,
  })
  services?: Service[];
  @ApiProperty({
    description: 'The method of DID.',
  })
  method: string;
  id?: string;
  @ApiProperty({
    description: 'The keypair type to be generated',
    enum: VerificationKeyType
  })
  keyPairType?: VerificationKeyType
}
