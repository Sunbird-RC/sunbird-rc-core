import { Module } from '@nestjs/common';
import { RenderingTemplatesService } from './rendering-templates.service';
import { RenderingTemplatesController } from './rendering-templates.controller';
import { PrismaService } from 'src/prisma.service';
import { ValidateTemplateService } from './validate-template.service';
import { SchemaModule } from 'src/schema/schema.module';
import { SchemaService } from 'src/schema/schema.service';

@Module({
  imports: [SchemaModule],
  providers: [RenderingTemplatesService, PrismaService, ValidateTemplateService, SchemaService],
  controllers: [RenderingTemplatesController],

})
export class RenderingTemplatesModule {}
