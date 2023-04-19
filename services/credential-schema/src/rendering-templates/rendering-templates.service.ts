import {Injectable, InternalServerErrorException, NotFoundException } from '@nestjs/common';
import { Template } from '@prisma/client';
import { type } from 'os';
import { PrismaService } from '../prisma.service';
import { AddTemplateDTO } from './dto/addTemplate.dto';
import { ValidateTemplateService } from './validate-template.service';

type templateResponse = {
  template: string,
  schemaId: string,
  templateId: string,
  createdBy: string,
  updatedBy: string,
  createdAt: string, 
  updatedAt: string
}

@Injectable()
export class RenderingTemplatesService {
  constructor(private prisma: PrismaService,private readonly verifier: ValidateTemplateService) {}

  async getTemplateBySchemaID(schemaID: string): Promise<templateResponse[]> {
    try {
      console.log(schemaID);
      const templates = await this.prisma.template.findMany({
        where: { schema: schemaID },
      });
      if (templates == null){
        throw new NotFoundException('Template not found');
      }
      return templates.map((template) => (
        {
          template: template.template,
          templateId: template.id,
          schemaId: template.schema,
          createdAt: template.createdAt.toDateString(),
          createdBy: template.createdBy,
          updatedAt: template.updatedAt.toDateString(),
          updatedBy: template.updatedBy,
        }
      ))
    } catch (err) {
      throw new InternalServerErrorException(err);
    }
  }

  async getTemplateById(id: string): Promise<templateResponse> {
    try {
      const template =  await this.prisma.template.findFirst({
        where: {
            id: id,
            deleted: false,
        },
      });
      if (template == null){
        throw new NotFoundException('Template not found');
      }
      return {
        template: template.template,
        templateId: template.id,
        schemaId: template.schema,
        createdAt: template.createdAt.toDateString(),
        createdBy: template.createdBy,
        updatedAt: template.updatedAt.toDateString(),
        updatedBy: template.updatedBy,
      };
    } catch (err) {
      throw new InternalServerErrorException(err);
    }
  }

  async addTemplate(addTemplateDto: AddTemplateDTO): Promise<templateResponse> {
    try {
      if(await this.verifier.verify(addTemplateDto.template, addTemplateDto.schema)){
        const template = await this.prisma.template.create({
          data: {
            schema: addTemplateDto.schema,
            template: addTemplateDto.template,
            type: addTemplateDto.type,
            //These need to be obtained properly, not sure from where
            // createdBy: '',
            // updatedBy: '',
          },
        });
        return {
          template: template.template,
          templateId: template.id,
          schemaId: template.schema,
          createdAt: template.createdAt.toDateString(),
          createdBy: template.createdBy,
          updatedAt: template.updatedAt.toDateString(),
          updatedBy: template.updatedBy,
        };
      }
      else{
        throw new InternalServerErrorException("Template-Schema mismatch, please check if fields in the incoming template match the fields in corresponding schema")
      }

      
    } catch (err) {
      throw new InternalServerErrorException(err);
    }
  }

  async updateTemplate(
    id: string,
    updateTemplateDto: AddTemplateDTO,
  ): Promise<templateResponse> { //returns the number of records affected by updatemany
    try {
      await this.prisma.template.updateMany({
        where: {
          id: id,
          deleted: false,
        },
        data: {
          schema: updateTemplateDto.schema,
          template: updateTemplateDto.template,
          type: updateTemplateDto.type,
        },
      });
      const template = await this.prisma.template.findUnique(
        {
          where:{
            id:id,
          }
        }
      );
      if (template == null){
        throw new NotFoundException('Template not found');
      }
      return {
        template: template.template,
        templateId: template.id,
        schemaId: template.schema,
        createdAt: template.createdAt.toDateString(),
        createdBy: template.createdBy,
        updatedAt: template.updatedAt.toDateString(),
        updatedBy: template.updatedBy,
      };
    } catch (err) {
      throw new InternalServerErrorException(err);
    }
  }
  async deleteTemplate(
    id: string
  ): Promise<Template> {
    try{
      const templateToBeDeleted = await this.prisma.template.findUnique({
        where: {
          id: id,
        }
      })
      if (templateToBeDeleted.deleted == true) {
        throw new NotFoundException('Record not found');
      }
      const template = await this.prisma.template.update({
        where: {
            id: id
        } ,
        data: {
          deleted: true,
        },
      });
      if (template.deleted == true) {
        return template
      }
    } catch (err) {
      throw new InternalServerErrorException(err);
    }
  }
}
