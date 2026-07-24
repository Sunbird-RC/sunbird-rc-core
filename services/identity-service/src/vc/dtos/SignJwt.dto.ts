import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';

export class SignJwtDTO {
  @ApiProperty({ description: 'The unique DID id of the issuer.' })
  DID: string;

  @ApiProperty({ description: 'JSON payload to sign as the JWT claim set.' })
  payload: object;

  @ApiPropertyOptional({
    description: "Extra protected-header fields, e.g. { typ: 'JWT' }. alg/kid are set by the service.",
  })
  header?: Record<string, any>;
}

export class VerifyJwtDTO {
  @ApiProperty({ description: 'Compact JWS/JWT to verify.' })
  jwt: string;

  @ApiPropertyOptional({
    description: 'Signer DID. Defaults to the DID part of the kid header.',
  })
  DID?: string;
}

export class SignSdJwtDTO {
  @ApiProperty({ description: 'The unique DID id of the issuer.' })
  DID: string;

  @ApiProperty({ description: 'Claim set for the SD-JWT.' })
  payload: Record<string, any>;

  @ApiPropertyOptional({
    description: 'Top-level claim names to make selectively disclosable.',
    type: [String],
  })
  disclosable?: string[];

  @ApiPropertyOptional({ description: 'Extra protected-header fields.' })
  header?: Record<string, any>;
}

export class VerifySdJwtDTO {
  @ApiProperty({ description: 'SD-JWT (issuer-jws~disclosure~...~[kb-jwt]).' })
  sdJwt: string;

  @ApiPropertyOptional({ description: 'Signer DID (defaults to kid header).' })
  DID?: string;

  @ApiPropertyOptional({
    description: 'Expected key-binding values, e.g. { nonce, audience }.',
  })
  keyBinding?: { nonce?: string; audience?: string };
}
