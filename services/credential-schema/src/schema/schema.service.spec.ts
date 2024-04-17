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

  it('should try to update ONLY metadata of a DRAFT schema', async () => {
    // create new schema
    const didBody = generateTestDIDBody();
    const did = await utilsService.generateDID(didBody);
    const credSchemaPayload = generateCredentialSchemaTestBody();
    credSchemaPayload.schema.author = did.id;
    const schema = await service.createCredentialSchema(credSchemaPayload);
    const updateSchemaPayload = {
      schema: null,
      status: null,
      tags: ['test1', 'test2'],
    };
    const updatedSchema = await service.updateCredentialSchema(
      {
        id_version: { id: schema.schema.id, version: schema.schema.version },
      },
      updateSchemaPayload,
    );
    expect(updatedSchema).toBeDefined();
    expect(updatedSchema.tags).toEqual(['test1', 'test2']);
    expect(updatedSchema.schema.version).toBe('1.0.1');
  });
  it('should try to update ONLY metadata of a PUBLISHED schema', async () => {
    const didBody = generateTestDIDBody();
    const did = await utilsService.generateDID(didBody);
    const credSchemaPayload = generateCredentialSchemaTestBody();
    credSchemaPayload.schema.author = did.id;
    const schema = await service.createCredentialSchema(credSchemaPayload);

    const pschema = await service.updateCredentialSchema(
      { id_version: { id: schema.schema.id, version: schema.schema.version } },
      { schema: null, tags: null, status: 'PUBLISHED' },
    );
    expect(pschema).toBeDefined();
    expect(pschema.status).toBe('PUBLISHED');

    const updateSchemaPayload = {
      schema: null,
      status: null,
      tags: ['test1', 'test2'],
    };
    const updatedSchema = await service.updateCredentialSchema(
      {
        id_version: { id: pschema.schema.id, version: pschema.schema.version },
      },
      updateSchemaPayload,
    );
    expect(updatedSchema).toBeDefined();
    expect(updatedSchema.tags).toEqual(['test1', 'test2']);
    expect(updatedSchema.schema.version).toBe('1.1.0');
    expect(updatedSchema.status).toBe('PUBLISHED');
  });
  it('should try updating the schema fields of a DRAFT Schema', async () => {
    const didBody = generateTestDIDBody();
    const did = await utilsService.generateDID(didBody);
    const credSchemaPayload = generateCredentialSchemaTestBody();
    credSchemaPayload.schema.author = did.id;
    const schema = await service.createCredentialSchema(credSchemaPayload);

    const updatedCredSchemaPayload = generateCredentialSchemaTestBody();
    updatedCredSchemaPayload.schema.author = schema.schema.id;
    const updatedSchema = await service.updateCredentialSchema(
      {
        id_version: { id: schema.schema.id, version: schema.schema.version },
      },
      { schema: updatedCredSchemaPayload.schema, status: null, tags: null },
    );

    expect(updatedSchema).toBeDefined();
    expect(updatedSchema.schema.version).toBe('1.1.0');
    expect(updatedSchema.schema.author).toBe(schema.schema.id);
  });
  it('should try updating the schema fields of a PUBLISHED Schema', async () => {
    const didBody = generateTestDIDBody();
    const did = await utilsService.generateDID(didBody);
    const credSchemaPayload = generateCredentialSchemaTestBody();
    credSchemaPayload.schema.author = did.id;
    const schema = await service.createCredentialSchema(credSchemaPayload);

    const pschema = await service.updateCredentialSchema(
      { id_version: { id: schema.schema.id, version: schema.schema.version } },
      { schema: null, tags: null, status: 'PUBLISHED' },
    );
    expect(pschema).toBeDefined();
    expect(pschema.status).toBe('PUBLISHED');

    const updatedCredSchemaPayload = generateCredentialSchemaTestBody();
    updatedCredSchemaPayload.schema.author = pschema.schema.id;

    const updatedSchema = await service.updateCredentialSchema(
      {
        id_version: { id: pschema.schema.id, version: pschema.schema.version },
      },
      { schema: updatedCredSchemaPayload.schema, status: null, tags: null },
    );

    expect(updatedSchema).toBeDefined();
    expect(updatedSchema.schema.version).toBe('2.0.0');
    expect(updatedSchema.status).toBe('PUBLISHED');
    expect(updatedSchema.schema.author).toBe(pschema.schema.id);
  });
});
