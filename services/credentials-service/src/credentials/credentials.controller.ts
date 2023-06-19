import {
  Body,
  Controller,
  Delete,
  Get,
  Query,
  Param,
  Post,
  Res,
} from '@nestjs/common';
import { CredentialsService } from './credentials.service';
import { GetCredentialsBySubjectOrIssuer } from './dto/getCredentialsBySubjectOrIssuer.dto';
import { IssueCredentialDTO } from './dto/issue-credential.dto';
import { RenderTemplateDTO } from './dto/renderTemplate.dto';
import { RENDER_OUTPUT } from './enums/renderOutput.enum';
import { Response } from 'express';
import { ApiQuery, ApiResponse, ApiTags } from '@nestjs/swagger';
import { string } from 'zod';

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
    status: 200,
    description: 'Successful operation',
  })
  @Get()
  getCredentials(@Query('tags') tags: string) {
    return this.credentialsService.getCredentials(tags.split(','));
  }

  @Post('/search')
  getCredentialsBySubject(@Body() getCreds: GetCredentialsBySubjectOrIssuer) {
    return this.credentialsService.getCredentialsBySubjectOrIssuer(getCreds);
  }

  @Get(':id')
  getCredentialById(@Param() id: { id: string }) {
    return this.credentialsService.getCredentialById(id?.id);
  }

  @Post('issue')
  issueCredentials(@Body() issueRequest: IssueCredentialDTO) {
    return this.credentialsService.issueCredential(issueRequest);
  }

  @Delete(':id')
  delteCredential(@Param('id') id: string) {
    return this.credentialsService.deleteCredential(id);
  }

  @Get(':id/verify')
  verifyCredential(@Param('id') credId: string) {
    return this.credentialsService.verifyCredential(credId);
  }

  @Post('render')
  async renderTemplate(
    @Body() renderRequest: RenderTemplateDTO,
    @Res({ passthrough: true }) response: Response,
  ) {
    let contentType = 'text/html';
    switch (renderRequest.output) {
      case RENDER_OUTPUT.PDF:
        contentType = 'application/pdf';
        break;
      case RENDER_OUTPUT.HTML:
        contentType = 'text/html';
        break;
    }
    response.header('Content-Type', contentType);
    return await this.credentialsService.renderCredential(renderRequest);
  }

  // TODO: Remove later and merge into cred-schema-ms
  @Get('schema/:id')
  async getSchemaByCredId(@Param('id') id: string) {
    return this.credentialsService.getSchemaByCredId(id);
  }
}
