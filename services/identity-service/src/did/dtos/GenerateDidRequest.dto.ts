import { ApiProperty } from '@nestjs/swagger';
const { Service } = require('did-resolver');
type Service = typeof Service;

export enum VerificationKeyType {
  Ed25519VerificationKey2020 = "Ed25519VerificationKey2020",
  Ed25519VerificationKey2018 = "Ed25519VerificationKey2018",
  RsaVerificationKey2018 = "RsaVerificationKey2018",
  // EC P-256, represented as a JsonWebKey2020 verification method — the key
  // type mso_mdoc (ISO/IEC 18013-5) mandates for its COSE_Sign1 (ES256)
  // issuer signature. Generated directly via Node's crypto (JWK export)
  // rather than a digitalbazaar LD-key library, since none of those cover
  // plain EC/JWK keys the way they do Ed25519/RSA.
  JsonWebKey2020 = "JsonWebKey2020"
}

export class GenerateDidDTO {
  @ApiProperty({
    description:
      'AlsoKnownAs property is a unique combination aadhaar and username.',
    type: String,
    isArray: true,
    required: false,
  })
  alsoKnownAs?: string[];

  @ApiProperty({
    description:
      'An array of services that are used, for example a user registration service.',
    isArray: true,
    required: false,
    type: Service
  })
  services?: Service[];
  @ApiProperty({
    description: 'The method of DID.',
  })
  method: string;
  @ApiProperty({
    required: false,
    description: 'Specific ID to generate DID document with.',
  })
  id?: string;
  @ApiProperty({
    required: false,
    description: 'In case of method "web" the web url path to access the did document. It would be appended by generated uuid',
  })
  webDidBaseUrl?: string;
  @ApiProperty({
    description: 'The keypair type to be generated',
    enum: VerificationKeyType
  })
  keyPairType?: VerificationKeyType;
}

export class GenerateDidRequestDTO {
  @ApiProperty({
    type: GenerateDidDTO,
    description: 'List of generate did requests',
    isArray: true
  })
  content: GenerateDidDTO[];
}
