import { Test, TestingModule } from '@nestjs/testing';
import { SchemaController } from './schema.controller';
import { HttpModule } from '@nestjs/axios';
import { SchemaService } from './schema.service';
import { CacheModule } from '@nestjs/common';

describe('SchemaController', () => {
  let controller: SchemaController;
  let schemaService: { [K in keyof SchemaService]?: jest.Mock };

  beforeEach(async () => {
    schemaService = {
      getOid4vciConfigs: jest.fn(),
      getCredentialSchemaByIdAndVersion: jest.fn(),
      getSchemaByTags: jest.fn(),
      getAllSchemasById: jest.fn(),
      createCredentialSchema: jest.fn(),
      updateCredentialSchema: jest.fn(),
      updateSchemaStatus: jest.fn(),
    };
    const module: TestingModule = await Test.createTestingModule({
      controllers: [SchemaController],
      imports: [HttpModule, CacheModule.register()],
      providers: [{ provide: SchemaService, useValue: schemaService }],
    }).compile();

    controller = module.get<SchemaController>(SchemaController);
  });

  it('should be defined', () => {
    expect(controller).toBeDefined();
  });

  it('getOid4vciConfigs delegates to the service', () => {
    schemaService.getOid4vciConfigs.mockResolvedValue(['config']);
    expect(controller.getOid4vciConfigs()).resolves.toEqual(['config']);
    expect(schemaService.getOid4vciConfigs).toHaveBeenCalled();
  });

  it('getCredentialSchemaByIdAndVersion delegates with id/version', async () => {
    schemaService.getCredentialSchemaByIdAndVersion.mockResolvedValue({ schema: {} });
    await controller.getCredentialSchemaByIdAndVersion('did:x', '1.0.0');
    expect(schemaService.getCredentialSchemaByIdAndVersion).toHaveBeenCalledWith({
      id_version: { id: 'did:x', version: '1.0.0' },
    });
  });

  it('getCredentialSchemaByTags splits tags and defaults page/limit', async () => {
    schemaService.getSchemaByTags.mockResolvedValue([]);
    await controller.getCredentialSchemaByTags('a,b', 'not-a-number', 'nope');
    expect(schemaService.getSchemaByTags).toHaveBeenCalledWith(['a', 'b'], 1, 10);
  });

  it('getCredentialSchemaByTags parses valid page/limit', async () => {
    schemaService.getSchemaByTags.mockResolvedValue([]);
    await controller.getCredentialSchemaByTags('a', '2', '5');
    expect(schemaService.getSchemaByTags).toHaveBeenCalledWith(['a'], 2, 5);
  });

  it('getAllSchemasWithId delegates with id', async () => {
    schemaService.getAllSchemasById.mockResolvedValue([]);
    await controller.getAllSchemasWithId('schema-id');
    expect(schemaService.getAllSchemasById).toHaveBeenCalledWith('schema-id');
  });

  it('createCredentialSchema delegates with body', () => {
    const body: any = { schema: {}, tags: [] };
    schemaService.createCredentialSchema.mockResolvedValue(body);
    controller.createCredentialSchema(body);
    expect(schemaService.createCredentialSchema).toHaveBeenCalledWith(body);
  });

  it('updateCredentialSchema delegates with id/version and body', () => {
    const body: any = { schema: null, status: null, tags: [] };
    schemaService.updateCredentialSchema.mockResolvedValue(body);
    controller.updateCredentialSchema('did:x', '1.0.0', body);
    expect(schemaService.updateCredentialSchema).toHaveBeenCalledWith(
      { id_version: { id: 'did:x', version: '1.0.0' } },
      body,
    );
  });

  it('deprecateSchema delegates with DEPRECATED status', async () => {
    schemaService.updateSchemaStatus.mockResolvedValue({});
    await controller.deprecateSchema('did:x', '1.0.0');
    expect(schemaService.updateSchemaStatus).toHaveBeenCalledWith(
      { id_version: { id: 'did:x', version: '1.0.0' } },
      'DEPRECATED',
    );
  });

  it('revokeSchema delegates with REVOKED status', async () => {
    schemaService.updateSchemaStatus.mockResolvedValue({});
    await controller.revokeSchema('did:x', '1.0.0');
    expect(schemaService.updateSchemaStatus).toHaveBeenCalledWith(
      { id_version: { id: 'did:x', version: '1.0.0' } },
      'REVOKED',
    );
  });

  it('publishSchema delegates with PUBLISHED status', async () => {
    schemaService.updateSchemaStatus.mockResolvedValue({});
    await controller.publishSchema('did:x', '1.0.0');
    expect(schemaService.updateSchemaStatus).toHaveBeenCalledWith(
      { id_version: { id: 'did:x', version: '1.0.0' } },
      'PUBLISHED',
    );
  });
});
