import {
  Injectable,
  InternalServerErrorException,
  Logger,
  NotFoundException,
} from '@nestjs/common';
import { PrismaClient, Template } from '@prisma/client';
import { AddTemplateDTO } from './dto/addTemplate.dto';
import { ValidateTemplateService } from './validate-template.service';
import { TemplateWarnings } from './types/TemplateWarnings.interface';

@Injectable()
export class RenderingTemplatesService {
  constructor(
    private prisma: PrismaClient,
    private readonly verifier: ValidateTemplateService,
  ) {}
  private logger = new Logger(RenderingTemplatesService.name);
  async getTemplateBySchemaID(schemaId: string): Promise<Template[]> {
    try {
      return await this.prisma.template.findMany({
        where: { schemaId },
      });
    } catch (err) {
      this.logger.error(err);
      throw new InternalServerErrorException(
        err,
        'Error fetching templates for the schemaID',
      );
    }
  }

  async getTemplateById(id: string): Promise<Template> {
    const template = await this.prisma.template.findUnique({
      where: { templateId: id },
    });
    if (!template)
      throw new NotFoundException('No template with the given id not found');
    return template;
  }

  async addTemplate(
    addTemplateDto: AddTemplateDTO,
  ): Promise<{ template: Template; warnings: TemplateWarnings }> {
    const warnings = await this.verifier.validateTemplateAgainstSchema(
      addTemplateDto.template,
      addTemplateDto.schemaId,
      addTemplateDto.schemaVersion,
    );
    try {
      const template = await this.prisma.template.create({
        data: {
          schemaId: addTemplateDto.schemaId,
          template: addTemplateDto.template,
          type: addTemplateDto.type,
        },
      });
      this.logger.log('Template added successfully');
      return { template, warnings };
    } catch (err) {
      this.logger.error(err);
      throw new InternalServerErrorException(err, 'Error adding template');
    }
  }

  async updateTemplate(
    id: string,
    updateTemplateDto: AddTemplateDTO,
  ): Promise<Template> {
    try {
      const template = await this.prisma.template.update({
        where: { templateId: id },
        data: {
          schemaId: updateTemplateDto.schemaId,
          template: updateTemplateDto.template,
          type: updateTemplateDto.type,
        },
      });
      this.logger.log('Template updated successfully');
      return template;
    } catch (err) {
      this.logger.error(err);
      throw new InternalServerErrorException(err, 'Error updating templates');
    }
  }
  async deleteTemplate(id: string): Promise<any> {
    try {
      await this.prisma.template.delete({
        where: { templateId: id },
      });
      this.logger.log('Template deleted successfully');
      return {
        message: 'Template deleted successfully',
      };
    } catch (err) {
      this.logger.error(err);
      throw new InternalServerErrorException(err, 'Error deleting template');
    }
  }
}
