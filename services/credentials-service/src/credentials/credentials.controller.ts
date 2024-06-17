import {
  Body,
  Controller,
  Delete,
  Get,
  Query,
  Param,
  Post,
  Res,
  BadRequestException,
  Req,
} from '@nestjs/common';
import { CredentialsService } from './credentials.service';
import { GetCredentialsBySubjectOrIssuer } from './dto/getCredentialsBySubjectOrIssuer.dto';
import { IssueCredentialDTO } from './dto/issue-credential.dto';
import { RenderTemplateDTO } from './dto/renderTemplate.dto';
import { RENDER_OUTPUT } from './enums/renderOutput.enum';
import { Request, Response } from 'express';
import { ApiBody, ApiQuery, ApiResponse, ApiTags } from '@nestjs/swagger';
import { string } from 'zod';
import { Credential } from 'src/app.interface';
import { GetCredentialsByTagsResponseDTO } from './dto/getCredentialsByTags.dto';
import { GetCredentialByIdResponseDTO } from './dto/getCredentialById.dto';
import { RevocationListDTO } from './dto/revocaiton-list.dto';
import { VerifyCredentialDTO } from './dto/verify-credential.dto';

@Controller('credentials')
export class CredentialsController {
  constructor(private readonly credentialsService: CredentialsService) {}

  @ApiTags('Issuing')
  @ApiQuery({
    name: 'tags',
    description: 'A comma separated string of tags to filter by',
    type: string,
  })
  @ApiResponse({
    type: GetCredentialsByTagsResponseDTO,
    status: 200,
    description: 'Successful operation',
  })
  @Get()
  getCredentials(
    @Query('tags') tags: string,
    @Query('page') page: string,
    @Query('limit') limit: string
  ) {
    return this.credentialsService.getCredentials(
      tags.split(','),
      isNaN(parseInt(page)) ? 1 : parseInt(page),
      isNaN(parseInt(limit)) ? 10 : parseInt(limit)
    );
  }

  @Post('/search')
  getCredentialsBySubject(
    @Body() getCreds: GetCredentialsBySubjectOrIssuer,
    @Query('page') page: string,
    @Query('limit') limit: string
  ) {
    return this.credentialsService.getCredentialsBySubjectOrIssuer(
      getCreds,
      isNaN(parseInt(page)) ? 1 : parseInt(page),
      isNaN(parseInt(limit)) ? 10 : parseInt(limit)
    );
  }

  @Get(':id')
  @ApiResponse({
    type: GetCredentialByIdResponseDTO,
    description: 'Returns a credential with the given id',
  })
  getCredentialById(@Param('id') id: string, @Req() req: Request) {
    const accept: string = req.headers['accept']?.trim() || 'application/json';
    const requestedTemplateId: string = req.headers['templateid'] as string;
    const externalTemplate: string = req.headers['template'] as string;

    if (!requestedTemplateId && !externalTemplate && accept !== 'application/json')
      throw new BadRequestException('Template id is required');
    else if (!requestedTemplateId && !externalTemplate && accept === 'application/json')
      return this.credentialsService.getCredentialById(
        id,
        requestedTemplateId,
        externalTemplate,
        RENDER_OUTPUT.JSON
      );

    let output = RENDER_OUTPUT.JSON;
    switch (accept) {
      case 'application/json':
        output = RENDER_OUTPUT.JSON;
        break;
      case 'application/pdf':
        output = RENDER_OUTPUT.PDF;
        break;
      case 'text/html':
        output = RENDER_OUTPUT.HTML;
        break;
      case 'text/plain':
        output = RENDER_OUTPUT.STRING;
        break;
      case 'image/svg+xml':
        output = RENDER_OUTPUT.QR;
    }
    return this.credentialsService.getCredentialById(id, requestedTemplateId, externalTemplate, output);
  }

  @Post('issue')
  issueCredentials(@Body() issueRequest: IssueCredentialDTO) {
    return this.credentialsService.issueCredential(issueRequest);
  }

  @Delete(':id')
  deleteCredential(@Param('id') id: string) {
    return this.credentialsService.deleteCredential(id);
  }

  @Get(':id/verify')
  verifyCredentialById(@Param('id') credId: string) {
    return this.credentialsService.verifyCredentialById(credId);
  }

  @Post('/verify')
  verifyCredential(@Body() verifyRequest: VerifyCredentialDTO) {
    return this.credentialsService.verifyCredential(verifyRequest.verifiableCredential);
  }

  @Get('revocation-list')
  @ApiResponse({
    type: Array<RevocationListDTO>,
    description: 'Returns a list of revocked credentials',
  })
  getRevocationList(
    @Query('issuerId') issuerId : string,
    @Query('page') page: string,
    @Query('limit') limit: string
  ) {

    return this.credentialsService.getRevocationList(
      issuerId,
      isNaN(parseInt(page)) ? 1 : parseInt(page),
      isNaN(parseInt(limit)) ? 1000 : parseInt(limit)
    );
  }
}
