import {
  Body,
  Controller,
  Delete,
  Get,
  Query,
  Param,
  Post,
  Res,
  StreamableFile,
} from '@nestjs/common';
import { CredentialsService } from './credentials.service';
import { GetCredentialsBySubjectOrIssuer } from './dto/getCredentialsBySubjectOrIssuer.dto';
import { IssueCredentialDTO } from './dto/issue-credential.dto';
import { RenderTemplateDTO } from './dto/renderTemplate.dto';
import { RENDER_OUTPUT } from './enums/renderOutput.enum';
import { UpdateStatusDTO } from './dto/update-status.dto';
import { DeriveCredentialDTO } from './dto/derive-credential.dto';
import { VerifyCredentialDTO } from './dto/verify-credential.dto';
import { Response } from 'express';

@Controller('credentials')
export class CredentialsController {
  constructor(private readonly credentialsService: CredentialsService) {}

  @Get()
  getCredentials(@Query('tags') tags: string) {
    // console.log('tags:', tags);
    return this.credentialsService.getCredentials(tags.split(','));
  }

  @Post('/search')
  getCredentialsBySubject(@Body() getCreds: GetCredentialsBySubjectOrIssuer) {
    return this.credentialsService.getCredentialsBySubjectOrIssuer(getCreds);
  }

  @Get(':id')
  getCredentialById(@Param() id: { id: string }) {
    // console.log('id in getByIdController: ', id);
    return this.credentialsService.getCredentialById(id?.id);
  }

  @Post('issue')
  issueCredentials(@Body() issueRequest: IssueCredentialDTO) {
    return this.credentialsService.issueCredential(issueRequest);
  }

  // @Post('status')
  // updateCredential(@Body() updateRequest: UpdateStatusDTO) {
  //   return this.credentialsService.updateCredential(updateRequest);
  // }

  @Delete(':id')
  delteCredential(@Param('id') id: string) {
    return this.credentialsService.deleteCredential(id);
  }

  @Get(':id/verify')
  verifyCredential(@Param('id') credId: string) {
    // console.log('credId: ', credId);
    return this.credentialsService.verifyCredential(credId);
  }

  // @Post('derive')
  // deriveCredential(@Body() deriveRequest: DeriveCredentialDTO) {
  //   return this.credentialsService.deriveCredential(deriveRequest);
  // }

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
    //response.contentType('appplication/pdf');
    // const res = console.log('res: ', res);
    // response.send(res);
    return await this.credentialsService.renderCredential(renderRequest);
  }

  // TODO: Remove later and merge into cred-schema-ms
  @Get('schema/:id')
  async getSchemaByCredId(@Param('id') id: string) {
    return this.credentialsService.getSchemaByCredId(id);
  }
}
