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
    }).compile();
    service = module.get<SchemaService>(SchemaService);
    utilsService = module.get<UtilsService>(UtilsService);
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
    expect(utilsService).toBeDefined();
  });

  it('should create a signed schema and get it from DB', async () => {
    const did = await utilsService.generateDID(testDIDBody);
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

  it('should try creating a new schema with a given did', () => {
    return;
  });

  it('should try adding a new schema without a given did', () => {
    return;
  });
  it('should try adding a schema with a version', () => {
    return;
  });
  it('should try adding a schema without a version', () => {
    return;
  });
  it('should try adding a schema with a version that does not follow semver', () => {
    return;
  });

  it('should try to update ONLY metadata of a DRAFT schema', () => {
    return;
  });
  it('should try to update ONLY metadata of a PUBLISHED schema', () => {
    return;
  });
  it('should try updating the schema fields of a DRAFT Schema', () => {
    return;
  });
  it('should try updating the schema fields of a PUBLISHED Schema', () => {
    return;
  });
});
