import { InternalServerErrorException } from '@nestjs/common';
import { ValidateTemplateService } from './validate-template.service';
import { SchemaService } from '../schema/schema.service';

describe('ValidateTemplateService', () => {
  let service: ValidateTemplateService;
  let schemaService: { getCredentialSchemaByIdAndVersion: jest.Mock };

  function withRequiredFields(requiredFields: string[]) {
    schemaService.getCredentialSchemaByIdAndVersion.mockResolvedValue({
      schema: { schema: { required: requiredFields } },
    });
  }

  beforeEach(() => {
    schemaService = { getCredentialSchemaByIdAndVersion: jest.fn() };
    service = new ValidateTemplateService(schemaService as unknown as SchemaService);
  });

  it('returns undefined when the template fields match the schema required fields', async () => {
    withRequiredFields(['grade', 'programme']);
    const result = await service.validateTemplateAgainstSchema(
      '{{grade}} {{programme}}',
      'schema-1',
      '1.0.0',
    );
    expect(result).toBeUndefined();
  });

  it('warns when the number of template fields does not match the required field count', async () => {
    withRequiredFields(['grade', 'programme']);
    const result = await service.validateTemplateAgainstSchema('{{grade}}', 'schema-1', '1.0.0');
    expect(result).toEqual({
      message: 'Number of fields in HBS file does not match required field list in schema',
      hsbsFields: ['{{grade}}'],
      requiredFields: ['grade', 'programme'],
    });
  });

  it('warns when the field names do not match the required fields', async () => {
    withRequiredFields(['grade', 'programme']);
    const result = await service.validateTemplateAgainstSchema(
      '{{grade}} {{institute}}',
      'schema-1',
      '1.0.0',
    );
    expect(result.message).toEqual(
      'Template not validated against schema successfully, strings do not match',
    );
  });

  it('throws InternalServerErrorException when the schema lookup fails', async () => {
    schemaService.getCredentialSchemaByIdAndVersion.mockRejectedValue(new Error('not found'));
    await expect(
      service.validateTemplateAgainstSchema('{{grade}}', 'schema-1', '1.0.0'),
    ).rejects.toThrow(InternalServerErrorException);
  });
});