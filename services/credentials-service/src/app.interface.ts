import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { W3CCredential } from 'did-jwt-vc';

export type Extensible<T> = T & { [x: string]: any };

export interface CredentialStatus {
  id: string;
  type: string;
}

export type JwtCredentialSubject = Record<string, any>;

export type Credential = Extensible<{
  '@context': string[] | string;
  type: string[] | string;
  issuer: string;
  credentialSubject: JwtCredentialSubject;
  credentialStatus?: CredentialStatus;
  termsOfUse?: any;
  proof?: string;
}>;

export class VCRequest {
  @ApiPropertyOptional({
    type: String,
    description: 'Issuer of the VC',
  })
  issuer?: string;
  @ApiProperty({
    type: String,
    description: 'Subject of the VC',
  })
  subject: string;
  @ApiProperty({
    type: String,
    description: 'Schema of the VC',
  })
  schema: string;
  @ApiProperty({
    type: String,
    description: 'Type of the VC',
  })
  type: string;
  @ApiProperty({
    type: String,
    description: 'Credential of the VC',
  })
  credential: W3CCredential;
}

export class IssueRequest {
  id: string;
}

export interface vc {
  '@context': Array<string>;
  type: Array<string>;
}
export interface credential {
  '@context': Array<string>;
  id: string;
  type: Array<string>;
  issuer: string;
  issueance_date: string;
  expiration_date: string;
  credentialSubject: any;
}

export interface credentialOptions {
  created: string;
  challenge: string;
  domain: string;
  credentialStatus: any;
}

export class VCResponse {
  @ApiProperty({
    type: String,
    description: 'Context of the VC',
  })
  '@context': Array<string>;
  @ApiProperty({
    type: String,
    description: 'ID of the VC',
  })
  id: string;
  @ApiProperty({
    type: String,
    description: 'Type of the VC',
  })
  type: string[];
  @ApiProperty({
    type: String,
    description: 'Issuer of the VC',
  })
  issuer: string;
  @ApiProperty({
    type: String,
    description: 'Date of issuance of the VC',
  })
  issuanceDate: Date;
  @ApiProperty({
    type: String,
    description: 'Subject of the VC',
  })
  credentialSubject: any;
  @ApiProperty({
    type: String,
    description: 'Proof of the VC',
  })
  proof: Proof;
}

export interface Proof {
  type: string;
  created?: Date;
  proofPurpose?: string;
  verificationMethod?: string;
  jws: string;
}

export class VCUpdateRequest {
  sub: string;
  iss: string;
  crdentialStatus: any;
}
