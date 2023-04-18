import {
  Controller,
  Get,
  HttpCode,
  Param,
  Post,
  Body,
  Render,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { ApiBody, ApiOperation, ApiResponse } from '@nestjs/swagger';

import { Verifiable, W3CCredential } from 'did-jwt-vc';
import { IssueRequest, VCRequest, VCResponse } from './app.interface';
import { AppService } from './app.service';

@Controller()
export class AppController {
  constructor(
    private readonly appService: AppService,
    private configService: ConfigService,
  ) {}

  @Get()
  handleHealthCheck(): string {
    return 'Hello World!';
  }

  // @ApiOperation({ summary: 'VC Claim' })
  // @ApiResponse({ type: VCRequest, status: 201, description: 'Create a new VC' })
  // @ApiBody({ type: VCResponse })
  // @Post('claim')
  // @HttpCode(201)
  // claim(@Body() vcRequest: VCRequest): any {
  //   return this.appService.claim(vcRequest);
  // }

  @ApiOperation({ summary: 'Sign a claim' })
  @ApiResponse({ type: VCRequest, status: 201, description: 'Create a new VC' })
  @ApiBody({ type: VCResponse })
  @Post('issuecred')
  @HttpCode(201)
  issue(@Body() issueRequest: IssueRequest): any {
    return this.appService.issue(issueRequest);
  }

  @ApiOperation({ summary: 'Get a Credential as a QR' })
  @Get('qr/:id')
  @Render('qrtemplate.hbs')
  async createQR(@Param('id') id) {
    return { image: await this.appService.renderAsQR(id) };
  }

  @ApiOperation({ summary: 'Render a Credential' })
  @Get('render/:id')
  @Render('credential.hbs')
  async renderCredential(@Param('id') id) {
    return { image: await this.appService.renderAsQR(id) };
  }

  // @ApiOperation({ summary: 'Get All VCs' })
  // @ApiResponse({ type: VCResponse, status: 200, description: 'Get list of all VCs' })
  // @Get()
  // getVCs(): Promise<any> {
  //   return this.appService.getVCs();
  // }

  // @ApiOperation({ summary: 'Get VC by Subject' })
  // @ApiResponse({ type: VCResponse, status: 200, description: 'Get VC details by Subject' })
  // @Get('/:sub')
  // @HttpCode(200)
  // getVCBySub(@Param('sub') sub: string): Promise<any> {
  //   return this.appService.getVCBySub(sub);
  // }

  // @ApiOperation({ summary: 'Get VC by Issuer' })
  // @ApiResponse({ type: VCResponse, status: 200, description: 'Get VC details by Issuer' })
  // @Get('/:iss')
  // @HttpCode(200)
  // getVCByIss(@Param('iss') iss: string): Promise<any> {
  //   return this.appService.getVCByIss(iss);
  // }

  // @ApiOperation({ summary: 'Update VC Status' })
  // @ApiResponse({ type: String, status: 200, description: 'Update VC' })
  // @ApiBody({ type: VCUpdateRequest })
  // @Post('status')
  // @HttpCode(200)
  // update(req: VCUpdateRequest, @Headers('X-AUTHORIZATION') token: string): any {
  //   return this.appService.updateStatus(req, token);
  // }

  @ApiOperation({ summary: 'Verify Credential' })
  @ApiResponse({ type: String, status: 200, description: 'Update VC' })
  @Post('verifycred')
  @HttpCode(200)
  verify(@Body() credential: Verifiable<W3CCredential>): any {
    return this.appService.verify(credential);
  }
}
