import { Controller, Get } from '@nestjs/common';
import { ApiOperation, ApiTags } from '@nestjs/swagger';
import { Oid4vciService } from './oid4vci.service';
import { TokenService } from './token.service';

// Public discovery endpoints. Wallets fetch openid-credential-issuer first;
// the Java registry / wallets fetch openid-configuration + jwks to trust tokens.
@ApiTags('OID4VCI-Discovery')
@Controller('.well-known')
export class MetadataController {
  constructor(
    private readonly oid4vci: Oid4vciService,
    private readonly tokens: TokenService,
  ) {}

  @ApiOperation({ summary: 'OID4VCI issuer metadata' })
  @Get('openid-credential-issuer')
  issuerMetadata() {
    return this.oid4vci.issuerMetadata();
  }

  @ApiOperation({ summary: 'OAuth 2.0 AS metadata (pre-authorized_code grant)' })
  @Get('openid-configuration')
  asMetadata() {
    return this.tokens.asMetadata();
  }

  @ApiOperation({ summary: 'Public JWKS (proxied from identity-service)' })
  @Get('jwks.json')
  jwks() {
    return this.tokens.jwks();
  }
}
