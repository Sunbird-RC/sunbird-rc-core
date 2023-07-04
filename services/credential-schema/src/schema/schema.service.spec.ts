import { Test, TestingModule } from '@nestjs/testing';
import { SchemaService } from './schema.service';
import { UtilsService } from '../utils/utils.service';
import { UtilsServiceMock } from '../utils/mock.util.service';
import { PrismaClient } from '@prisma/client';
import { credentialSchemaDemoPayload } from './schema.fixtures';
describe('SchemaService', () => {
  let service: SchemaService;
  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [SchemaService, UtilsService, PrismaClient],
    })
      .overrideProvider(UtilsService)
      .useClass(UtilsServiceMock)
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
