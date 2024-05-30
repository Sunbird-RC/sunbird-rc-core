import { ApiProperty } from '@nestjs/swagger';
const { Service } = require('did-resolver');
type Service = typeof Service;

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
}

export class GenerateDidRequestDTO {
  @ApiProperty({
    type: GenerateDidDTO,
    description: 'List of generate did requests',
    isArray: true
  })
  content: GenerateDidDTO[]
}
