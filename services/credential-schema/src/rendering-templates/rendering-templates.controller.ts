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

@Controller('rendering-template')
export class RenderingTemplatesController {
  constructor(
    private readonly renderingTemplateService: RenderingTemplatesService,
  ) {}

  @Get()
  getTemplateBySchemaID(@Query('schemaId') schemaId: string) {
    return this.renderingTemplateService.getTemplateBySchemaID(schemaId);
  }

  @Get(':templateId')
  getTemplateById(@Param('templateId') id: string) {
    return this.renderingTemplateService.getTemplateById(id);
  }

  @Post()
  addTemplate(@Body() addTemplateDto: AddTemplateDTO) {
    return this.renderingTemplateService.addTemplate(addTemplateDto);
  }

  @Put(':templateId')
  updateTemplate(
    @Body() updateTemplateDto: UpdateTemplateDTO,
    @Param('templateId') id: string,
  ) {
    return this.renderingTemplateService.updateTemplate(id, updateTemplateDto);
  }
  @Delete(':templateId')
  deleteTemplate(@Param('templateId') id: string) {
    return this.renderingTemplateService.deleteTemplate(id);
  }
}
