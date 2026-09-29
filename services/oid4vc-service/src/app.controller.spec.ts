import { NotFoundException } from '@nestjs/common';
import { AppController } from './app.controller';

describe('AppController', () => {
  const ORIGINAL_ENV = process.env;
  let schema: any;
  let oid4vci: any;
  let keycloak: any;
  let controller: AppController;

  beforeEach(() => {
    process.env = { ...ORIGINAL_ENV };
    process.env.PUBLIC_URL = 'https://issuer.example';
    schema = { getOid4vciConfigs: jest.fn() };
    oid4vci = { getVctTypeMetadata: jest.fn() };
    keycloak = { healthInfo: jest.fn() };
    controller = new AppController(schema, oid4vci, keycloak);
  });

  afterEach(() => {
    process.env = ORIGINAL_ENV;
  });

  describe('health', () => {
    it('reports UP with the keycloak health info embedded', async () => {
      keycloak.healthInfo.mockResolvedValue({ status: 'UP' });
      const result = await controller.health();
      expect(result).toEqual({
        status: 'UP',
        service: 'oid4vc-service',
        keycloak: { status: 'UP' },
      });
    });
  });

  describe('renderTemplate', () => {
    it('returns the inline SVG for the matching schema config', async () => {
      schema.getOid4vciConfigs.mockResolvedValue([
        { schemaId: 'other', renderMethod: { svg: '<svg>other</svg>' } },
        { schemaId: 'schema-1', renderMethod: { svg: '<svg>mine</svg>' } },
      ]);

      const result = await controller.renderTemplate('schema-1');
      expect(result).toBe('<svg>mine</svg>');
    });

    it('throws NotFoundException when no config matches the schemaId', async () => {
      schema.getOid4vciConfigs.mockResolvedValue([
        { schemaId: 'other', renderMethod: { svg: '<svg/>' } },
      ]);

      await expect(controller.renderTemplate('schema-1')).rejects.toThrow(NotFoundException);
    });

    it('throws NotFoundException when the matching config has no renderMethod.svg', async () => {
      schema.getOid4vciConfigs.mockResolvedValue([{ schemaId: 'schema-1' }]);

      await expect(controller.renderTemplate('schema-1')).rejects.toThrow(NotFoundException);
    });
  });

  describe('context', () => {
    it('returns a JSON-LD context document keyed by the type name', () => {
      const result = controller.context('MyType');
      expect(result).toEqual({
        '@context': {
          '@vocab': 'https://issuer.example/vocab#',
          MyType: 'https://issuer.example/vocab#MyType',
        },
      });
    });
  });

  describe('vctMetadata', () => {
    it('delegates to oid4vci.getVctTypeMetadata with the slug', async () => {
      const expected = { vct: 'https://issuer.example/vct/national-id' };
      oid4vci.getVctTypeMetadata.mockResolvedValue(expected);

      const result = await controller.vctMetadata('national-id');
      expect(result).toBe(expected);
      expect(oid4vci.getVctTypeMetadata).toHaveBeenCalledWith('national-id');
    });
  });
});