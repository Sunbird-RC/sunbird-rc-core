import { Test, TestingModule } from '@nestjs/testing';
import { RenderingTemplatesController } from './rendering-templates.controller';
import { RenderingTemplatesService } from './rendering-templates.service';

describe('RenderingTemplatesController', () => {
  let controller: RenderingTemplatesController;
  let service: { [K in keyof RenderingTemplatesService]?: jest.Mock };

  beforeEach(async () => {
    service = {
      getTemplateBySchemaID: jest.fn(),
      getTemplateById: jest.fn(),
      addTemplate: jest.fn(),
      updateTemplate: jest.fn(),
      deleteTemplate: jest.fn(),
    };
    const module: TestingModule = await Test.createTestingModule({
      controllers: [RenderingTemplatesController],
      providers: [{ provide: RenderingTemplatesService, useValue: service }],
    }).compile();

    controller = module.get<RenderingTemplatesController>(
      RenderingTemplatesController,
    );
  });

  it('should be defined', () => {
    expect(controller).toBeDefined();
  });

  it('getTemplateBySchemaID delegates with schemaId', async () => {
    service.getTemplateBySchemaID.mockResolvedValue([]);
    await controller.getTemplateBySchemaID('schema-1');
    expect(service.getTemplateBySchemaID).toHaveBeenCalledWith('schema-1');
  });

  it('getTemplateById delegates with id', async () => {
    service.getTemplateById.mockResolvedValue({});
    await controller.getTemplateById('template-1');
    expect(service.getTemplateById).toHaveBeenCalledWith('template-1');
  });

  it('addTemplate delegates with body', async () => {
    const dto: any = { schemaId: 's', template: 't', type: 'x' };
    service.addTemplate.mockResolvedValue({ template: dto });
    await controller.addTemplate(dto);
    expect(service.addTemplate).toHaveBeenCalledWith(dto);
  });

  it('updateTemplate delegates with id and body', async () => {
    const dto: any = { schemaId: 's', template: 't', type: 'x' };
    service.updateTemplate.mockResolvedValue(dto);
    await controller.updateTemplate(dto, 'template-1');
    expect(service.updateTemplate).toHaveBeenCalledWith('template-1', dto);
  });

  it('deleteTemplate delegates with id', async () => {
    service.deleteTemplate.mockResolvedValue({ message: 'deleted' });
    await controller.deleteTemplate('template-1');
    expect(service.deleteTemplate).toHaveBeenCalledWith('template-1');
  });
});
