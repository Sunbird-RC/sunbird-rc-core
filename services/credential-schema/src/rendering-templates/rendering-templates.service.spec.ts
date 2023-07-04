import { Test, TestingModule } from '@nestjs/testing';
import { RenderingTemplatesService } from './rendering-templates.service';
import { ValidateTemplateService } from './validate-template.service';
import { SchemaService } from '../schema/schema.service';
import { PrismaClient } from '@prisma/client';
import { UtilsService } from '../utils/utils.service';
import { HttpModule } from '@nestjs/axios';
describe('RenderingTemplatesService', () => {
  let service: RenderingTemplatesService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      imports: [HttpModule],
      providers: [
        RenderingTemplatesService,
        PrismaClient,
        ValidateTemplateService,
        SchemaService,
        UtilsService,
      ],
    }).compile();

    service = module.get<RenderingTemplatesService>(RenderingTemplatesService);
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });
});
