import { Body, Controller, Get, HttpCode, Param, Post, Header } from '@nestjs/common';
import { ApiOperation, ApiTags } from '@nestjs/swagger';
import { Oid4vpService } from './oid4vp.service';

// OID4VP verifier-role endpoints, all under /vp/*.
@ApiTags('OID4VP')
@Controller('vp')
export class Oid4vpController {
  constructor(private readonly oid4vp: Oid4vpService) {}

  @ApiOperation({ summary: 'Verifier creates a presentation request (DCQL)' })
  @Post('request')
  createRequest(@Body() body: any) {
    return this.oid4vp.createRequest(body || {});
  }

  @ApiOperation({ summary: 'Wallet fetches the (unsigned, redirect_uri-scheme) request object' })
  @Get('request-object/:id')
  @Header('content-type', 'application/json')
  getRequestObject(@Param('id') id: string) {
    return this.oid4vp.getRequestObject(id);
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
