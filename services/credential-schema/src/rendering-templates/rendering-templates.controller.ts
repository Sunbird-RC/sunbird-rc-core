import {
  Body,
  Controller,
  Delete,
  Get,
  Param,
  Post,
  Put,
  Query,
} from '@nestjs/common';
import { AddTemplateDTO } from './dto/addTemplate.dto';
import { UpdateTemplateDTO } from './dto/updateTemplate.dto';
import { RenderingTemplatesService } from './rendering-templates.service';
import {
  ApiBadRequestResponse,
  ApiBody,
  ApiCreatedResponse,
  ApiNotFoundResponse,
  ApiOkResponse,
  ApiOperation,
  ApiQuery,
} from '@nestjs/swagger';
import { Template } from './entities/Template.entity';
import { TemplateBody } from './entities/TemplateBody.entity';

@Controller('template')
export class RenderingTemplatesController {
  constructor(
    private readonly renderingTemplateService: RenderingTemplatesService,
  ) {}

  @Get()
  @ApiQuery({ name: 'schemaId', required: true, type: String })
  @ApiOperation({ summary: 'Get a Template by schemaId' })
  @ApiOkResponse({
    status: 200,
    description: 'The record was found.',
    type: Template,
  })
  @ApiNotFoundResponse({
    status: 404,
    description: 'The record has not been found.',
  })
  getTemplateBySchemaID(@Query('schemaId') schemaId: string) {
    return this.renderingTemplateService.getTemplateBySchemaID(schemaId);
  }

  @ApiQuery({ name: 'templateId', required: true, type: String })
  @ApiOperation({ summary: 'Get a Template by templateId' })
  @ApiOkResponse({
    status: 200,
    description: 'The record has been successfully created.',
    type: Template,
  })
  @ApiNotFoundResponse({
    status: 404,
    description: 'The record has not been found.',
  })
  @Get(':templateId')
  getTemplateById(@Param('templateId') id: string) {
    return this.renderingTemplateService.getTemplateById(id);
  }

  @ApiOperation({ summary: 'Create a new Template' })
  @ApiBody({
    type: TemplateBody,
  })
  @ApiCreatedResponse({
    status: 201,
    description: 'The record has been successfully created.',
    type: Template,
  })
  @ApiBadRequestResponse({
    status: 400,
    description: 'There was some problem with the request.',
  })
  @Post()
  addTemplate(@Body() addTemplateDto: AddTemplateDTO) {
    return this.renderingTemplateService.addTemplate(addTemplateDto);
  }

  @ApiOperation({ summary: 'Update the template by templateId' })
  @ApiBody({
    type: TemplateBody,
  })
  @ApiOkResponse({
    status: 201,
    description: 'The record has been successfully udpated.',
    type: Template,
  })
  @ApiBadRequestResponse({
    status: 400,
    description: 'There was some problem with the request.',
  })
  @ApiQuery({ name: 'templateId', required: true, type: String })
  @Put(':templateId')
  updateTemplate(
    @Body() updateTemplateDto: UpdateTemplateDTO,
    @Param('templateId') id: string,
  ) {
    return this.renderingTemplateService.updateTemplate(id, updateTemplateDto);
  }

  @ApiOperation({ summary: 'Delete the template by templateId' })
  @ApiOkResponse({
    status: 200,
    description: 'The record has been successfully deleted.',
    type: Template,
  })
  @ApiBadRequestResponse({
    status: 400,
    description: 'There was some problem with the request.',
  })
  @ApiQuery({ name: 'templateId', required: true, type: String })
  @Delete(':templateId')
  deleteTemplate(@Param('templateId') id: string) {
    return this.renderingTemplateService.deleteTemplate(id);
  }
}
