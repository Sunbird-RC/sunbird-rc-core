import { Test, TestingModule } from '@nestjs/testing';
import { SchemaService } from './schema.service';
import { UtilsService } from '../utils/utils.service';
import { PrismaClient } from '@prisma/client';
import { HttpModule } from '@nestjs/axios';
import { credentialSchemaDemoPayload, testDIDBody } from './schema.fixtures';

describe('SchemaService', () => {
  let service: SchemaService;
  let utilsService: UtilsService;
  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      imports: [HttpModule],
      providers: [SchemaService, UtilsService, PrismaClient],
    })
      .compile();
    service = module.get<SchemaService>(SchemaService);
    utilsService = module.get<UtilsService>(UtilsService)
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
    expect(utilsService).toBeDefined();
  });

  it('should create a signed schema and get it from DB', async () => {
    
    const did = await utilsService.generateDID(testDIDBody)
    credentialSchemaDemoPayload.schema.author = did.id;
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
