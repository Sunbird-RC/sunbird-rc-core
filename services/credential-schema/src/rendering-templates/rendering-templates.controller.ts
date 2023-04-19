import { Body, ConsoleLogger, Controller, Delete, Get, Param, Post, Put, Query } from '@nestjs/common';
import { ApiOkResponse, ApiOperation } from '@nestjs/swagger';
import { AddTemplateDTO } from './dto/addTemplate.dto';
import { UpdateTemplateDTO } from './dto/updateTemplate.dto';
import { RenderingTemplatesService } from './rendering-templates.service';

@Controller('template')
export class RenderingTemplatesController {
  constructor(
    private readonly renderingTemplateService: RenderingTemplatesService,
  ) {}

  @Get()
  @ApiOperation(
    { summary: "GET Templates by schema ID" }
  )
  @ApiOkResponse(
    { status: 200 }
  )
  getTemplateBySchemaID(@Query('schemaId') schemaId: string) {
    return this.renderingTemplateService.getTemplateBySchemaID(schemaId);
  }

  @Get(':id')
  @ApiOperation(
    { summary: "GET Template by TemplateID" }
  )
  @ApiOkResponse(
    { status: 200 }
  )
  getTemplateById(@Param('id') id: string) {
    return this.renderingTemplateService.getTemplateById(id);
  }

  @Post()
  @ApiOperation(
    { summary: "Add new Template" }
  )
  @ApiOkResponse(
    {
      status: 201,
      description: "Rendering template successfully created!"
    }
  )
  addTemplate(@Body() addTemplateDto: AddTemplateDTO) {
    return this.renderingTemplateService.addTemplate(addTemplateDto); 
  }

  @Put(':id')
  @ApiOperation(
    { summary: "Update Template" }
  )
  @ApiOkResponse(
    { 
      status: 200,
      description: "rendering template updated successfully"
    }
  )
  updateTemplate(@Param('id') id: string, @Body() updateTemplateDto: UpdateTemplateDTO) {
    return this.renderingTemplateService.updateTemplate(
      id,
      updateTemplateDto,
    );
  }
  @Delete(':id')
  deleteTemplate(@Param('id') id:string) {
    if(this.renderingTemplateService.deleteTemplate(id)){
      return "Credential Schema successfully deleted!";
    };
  }
}
