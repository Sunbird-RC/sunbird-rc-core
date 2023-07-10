import { Test, TestingModule } from '@nestjs/testing';
import { SchemaService } from './schema.service';
import { UtilsService } from '../utils/utils.service';
import { PrismaClient } from '@prisma/client';
import { HttpModule } from '@nestjs/axios';
import {
  generateCredentialSchemaTestBody,
  generateTestDIDBody,
} from './schema.fixtures';

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

  it('should try creating a new schema', async () => {
    const didBody = generateTestDIDBody();
    const did = await utilsService.generateDID(didBody);
    const credSchemaPayload = generateCredentialSchemaTestBody();
    credSchemaPayload.schema.author = did.id;
    const vcCredSchema = await service.createCredentialSchema(
      credSchemaPayload,
    );
    expect(vcCredSchema).toBeDefined();
    expect(vcCredSchema.schema.proof).toBeTruthy();
    const getVCCredSchema = await service.getCredentialSchemaByIdAndVersion({
      id_version: {
        id: vcCredSchema.schema.id,
        version: '1.0.0',
      },
    });
    expect(getVCCredSchema).toBeTruthy();
    expect(getVCCredSchema.schema.version).toBe(vcCredSchema.schema.version);
  });

  it('should try adding a schema with a version', () => {
    return;
  });
  it('should try adding a schema without a version', async () => {
    const didBody = generateTestDIDBody();
    const did = await utilsService.generateDID(didBody);
    const credSchemaPayload = generateCredentialSchemaTestBody();
    delete credSchemaPayload.schema.version;
    credSchemaPayload.schema.author = did.id;
    const vcCredSchema = await service.createCredentialSchema(
      credSchemaPayload,
    );
    expect(vcCredSchema).toBeDefined();
    expect(vcCredSchema.schema.proof).toBeTruthy();
    const getVCCredSchema = await service.getCredentialSchemaByIdAndVersion({
      id_version: {
        id: vcCredSchema.schema.id,
        version: '0.0.0',
      },
    });
    expect(getVCCredSchema).toBeTruthy();
    expect(getVCCredSchema.schema.version).toBe(vcCredSchema.schema.version);
  });
  it('should try adding a schema with a version that does not follow semver', async () => {
    const didBody = generateTestDIDBody();
    const did = await utilsService.generateDID(didBody);
    const credSchemaPayload = generateCredentialSchemaTestBody();
    credSchemaPayload.schema.version = '1.0';
    credSchemaPayload.schema.author = did.id;
    expect(
      service.createCredentialSchema(credSchemaPayload),
    ).rejects.toThrowError();
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
