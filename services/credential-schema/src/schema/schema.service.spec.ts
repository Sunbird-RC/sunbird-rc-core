import { Test, TestingModule } from '@nestjs/testing';
import { SchemaService } from './schema.service';
import { UtilsService } from '../utils/utils.service';
import { PrismaClient } from '@prisma/client';
import { HttpModule } from '@nestjs/axios';
import { credentialSchemaDemoPayload } from './schema.fixtures';
describe('SchemaService', () => {
  let service: SchemaService;
  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      imports: [HttpModule],
      providers: [SchemaService, UtilsService, PrismaClient],
    })
      .compile();
    service = module.get<SchemaService>(SchemaService);
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });

  it('should create a signed schema and get it from DB', async () => {
    const vcCredSchema = await service.createCredentialSchema(
      credentialSchemaDemoPayload,
    );
    expect(vcCredSchema).toBeDefined();
    expect(vcCredSchema.schema.proof).toBeTruthy();
    const getVCCredSchema = await service.getCredentialSchema({
      id: vcCredSchema.schema.id,
    });
    expect(getVCCredSchema.schema.proof).toBeTruthy();
  });
});
