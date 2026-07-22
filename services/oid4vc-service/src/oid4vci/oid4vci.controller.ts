import {
  Body,
  Controller,
  Get,
  Headers,
  HttpCode,
  Param,
  Post,
  Res,
} from '@nestjs/common';
import { ApiOperation, ApiTags } from '@nestjs/swagger';
import { FastifyReply } from 'fastify';
import { Oid4vciService } from './oid4vci.service';

// OID4VCI issuer-role endpoints, all under /oid4vc/*.
@ApiTags('OID4VCI')
@Controller('oid4vc')
export class Oid4vciController {
  constructor(private readonly oid4vci: Oid4vciService) {}

  @ApiOperation({ summary: 'Create a credential offer (internal, called by issuer/registry)' })
  @Post('offer')
  createOffer(@Body() body: any) {
    return this.oid4vci.createOffer(body);
  }

  @ApiOperation({ summary: 'Dereference a credential offer (wallet)' })
  @Get('offer/:id')
  getOffer(@Param('id') id: string) {
    return this.oid4vci.getOffer(id);
  }

  @ApiOperation({ summary: 'Token endpoint (pre-authorized_code grant only)' })
  @Post('token')
  @HttpCode(200)
  token(@Body() body: any) {
    return this.oid4vci.token(body || {});
  }

  @ApiOperation({ summary: 'Issue a single-use c_nonce' })
  @Post('nonce')
  @HttpCode(200)
  async nonce() {
    return { c_nonce: await this.oid4vci.issueNonce() };
  }

  @ApiOperation({ summary: 'Credential endpoint — verifies PoP, issues signed VC' })
  @Post('credential')
  @HttpCode(200)
  credential(@Headers('authorization') auth: string, @Body() body: any) {
    return this.oid4vci.credential(auth, body || {});
  }

  @ApiOperation({ summary: 'Deferred credential endpoint (poll)' })
  @Post('deferred')
  async deferred(
    @Headers('authorization') auth: string,
    @Body() body: any,
    @Res({ passthrough: true }) res: FastifyReply,
  ) {
    const result = await this.oid4vci.deferred(auth, body || {});
    if ((result as any).pending) {
      res.status(202);
    }
    return result;
  }

  @ApiOperation({ summary: 'Wallet notification (accept/deny telemetry)' })
  @Post('notification')
  @HttpCode(204)
  notification(@Headers('authorization') auth: string, @Body() body: any) {
    return this.oid4vci.notification(auth, body || {});
  }
}
