import { Controller, Get } from '@nestjs/common';
import { ApiOkResponse, ApiOperation, ApiTags } from '@nestjs/swagger';
import { JwtSignerService } from '../vc/jwt.service';
import { Public } from '../auth/public.decorator';

@ApiTags('WellKnown')
@Controller('.well-known')
export class WellKnownController {
  constructor(private readonly jwtSigner: JwtSignerService) {}

  @ApiOperation({ summary: 'Public JWKS for all DIDs holding JWK verification methods' })
  @ApiOkResponse({ description: 'JWKS document' })
  @Public()
  @Get('jwks.json')
  getJwks() {
    return this.jwtSigner.getJwks();
  }
}
