import { Test, TestingModule } from '@nestjs/testing';
import { RenderingTemplatesService } from './rendering-templates.service';

describe('RenderingTemplatesService', () => {
  let service: RenderingTemplatesService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [RenderingTemplatesService],
    }).compile();

    service = module.get<RenderingTemplatesService>(RenderingTemplatesService);
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });
});
