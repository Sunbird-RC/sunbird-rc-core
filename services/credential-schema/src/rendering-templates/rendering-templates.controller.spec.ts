import { Test, TestingModule } from '@nestjs/testing';
import { RenderingTemplatesController } from './rendering-templates.controller';

describe('RenderingTemplatesController', () => {
  let controller: RenderingTemplatesController;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      controllers: [RenderingTemplatesController],
    }).compile();

    controller = module.get<RenderingTemplatesController>(RenderingTemplatesController);
  });

  it('should be defined', () => {
    expect(controller).toBeDefined();
  });
});
