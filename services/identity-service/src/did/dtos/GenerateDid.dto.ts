import { ApiProperty } from '@nestjs/swagger';
import { Service } from 'did-resolver';

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
  service?: Service[];
  @ApiProperty({
    description: 'The method of DID.',
  })
  method: string;
}
