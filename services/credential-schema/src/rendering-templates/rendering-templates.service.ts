import {
  Injectable,
  InternalServerErrorException,
  NotFoundException,
} from '@nestjs/common';
import { Template } from '@prisma/client';
import { PrismaService } from '../prisma.service';
import { AddTemplateDTO } from './dto/addTemplate.dto';
import { ValidateTemplateService } from './validate-template.service';

@Injectable()
export class RenderingTemplatesService {
  constructor(
    private prisma: PrismaService,
    private readonly verifier: ValidateTemplateService,
  ) {}

  async getTemplateBySchemaID(schemaId: string): Promise<Template[]> {
    try {
      return await this.prisma.template.findMany({
        where: { schemaId },
      });
    } catch (err) {
      throw new InternalServerErrorException(err);
    }
  }

  async getTemplateById(id: string): Promise<Template> {
    const temp = await this.prisma.template.findUnique({
      where: { templateId: id },
    });
    if (!temp)
      throw new NotFoundException('No template with the given id not found');
    return temp;
  }

  async addTemplate(addTemplateDto: AddTemplateDTO): Promise<Template> {
    try {
      if (
        await this.verifier.validateTemplateAgainstSchema(
          addTemplateDto.template,
          addTemplateDto.schemaId,
        )
      ) {
        return await this.prisma.template.create({
          data: {
            schemaId: addTemplateDto.schemaId,
            template: addTemplateDto.template,
            type: addTemplateDto.type,
          },
        });
      } else {
        throw new InternalServerErrorException(
          'Template-Schema mismatch, please check if fields in the incoming template match the fields in corresponding schema',
        );
      }
    } catch (err) {
      throw new InternalServerErrorException(err);
    }
  }

  async updateTemplate(
    id: string,
    updateTemplateDto: AddTemplateDTO,
  ): Promise<Template> {
    try {
      return await this.prisma.template.update({
        where: { templateId: id },
        data: {
          schemaId: updateTemplateDto.schemaId,
          template: updateTemplateDto.template,
          type: updateTemplateDto.type,
        },
      });
    } catch (err) {
      throw new InternalServerErrorException(err);
    }
  }
  async deleteTemplate(id: string): Promise<any> {
    try {
      await this.prisma.template.delete({
        where: { templateId: id },
      });

      return 'Template deleted successfully';
    } catch (err) {
      throw new InternalServerErrorException(err);
    }
  }
}
