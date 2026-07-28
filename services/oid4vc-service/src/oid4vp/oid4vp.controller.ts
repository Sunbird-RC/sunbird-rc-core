import {
  Body,
  Controller,
  Get,
  Headers,
  HttpCode,
  NotAcceptableException,
  Param,
  Post,
  Res,
} from '@nestjs/common';
import { ApiOperation, ApiTags } from '@nestjs/swagger';
import { FastifyReply } from 'fastify';
import { Oid4vpService } from './oid4vp.service';

// OID4VP verifier-role endpoints, all under /vp/*.
@ApiTags('OID4VP')
@Controller('vp')
export class Oid4vpController {
  constructor(private readonly oid4vp: Oid4vpService) {}

  @ApiOperation({
    summary:
      'Verifier creates a presentation request (DCQL). Signed draft-23 JAR by default; ' +
      'pass {"signed": false} for an unsigned request, or set OID4VP_LEGACY_CLIENT_ID_SCHEME ' +
      'for the pre-draft-22 redirect_uri client_id_scheme shape.',
  })
  @Post('request')
  createRequest(@Body() body: any) {
    return this.oid4vp.createRequest(body || {});
  }

  @ApiOperation({
    summary:
      'Wallet fetches the request object. Signed transactions return a JWS ' +
      '(application/oauth-authz-req+jwt); unsigned/legacy transactions return plain JSON.',
  })
  @Get('request-object/:id')
  async getRequestObject(
    @Param('id') id: string,
    @Headers('accept') accept: string | undefined,
    @Res({ passthrough: true }) res: FastifyReply,
  ) {
    const { body, contentType } = await this.oid4vp.getRequestObject(id);
    // The signing mode was fixed at createRequest time (it's already baked
    // into the client_id in the QR deep link), so this can't silently
    // downgrade to a representation the wallet didn't ask for — reject
    // instead of negotiating.
    if (accept && accept !== '*/*' && !accept.includes(contentType) && !accept.includes('*/*')) {
      throw new NotAcceptableException(
        `this request object is only available as ${contentType}`,
      );
    }
    res.header('content-type', contentType);
    return body;
  }

  @ApiOperation({ summary: 'Wallet submits the VP token (direct_post)' })
  @Post('response')
  @HttpCode(200)
  submitResponse(@Body() body: any) {
    return this.oid4vp.submitResponse(body || {});
  }

  @ApiOperation({ summary: 'Verifier polls the verification result' })
  @Get('status/:id')
  getStatus(@Param('id') id: string) {
    return this.oid4vp.getStatus(id);
  }
}
