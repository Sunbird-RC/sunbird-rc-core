import { Test, TestingModule } from '@nestjs/testing';
import { RenderingTemplatesController } from './rendering-templates.controller';
import { RenderingTemplatesService } from './rendering-templates.service';
import { ModuleMocker, MockFunctionMetadata } from 'jest-mock';
import * as request from 'supertest'
const moduleMocker = new ModuleMocker(global);

describe('RenderingTemplatesController', () => {
  let controller: RenderingTemplatesController;
  let id:any;
  // let spyService: RenderingTemplatesService

  beforeEach(async () => {
    // const mockTemplatesProvider = {
    //   provide: RenderingTemplatesService,
    //   useFactory: () => ({
    //     getTemplateBySchemaID: jest.fn(() => []),
    //     getTemplateById: jest.fn(() => []),
    //     addTemplate: jest.fn(() => []),
    //     updateTemplate: jest.fn(() => []),
    //     deleteTemplate: jest.fn(() => [])
    //   })
    // }
    // const mockPrismaProvider = {
    //   provide: PrismaService,
    //   useFactory: () => ({

    //   })
    // }

    const mockTemplatesProvider = {
      provide: RenderingTemplatesService,
      useFactory: () => ({
        getTemplateBySchemaID: jest.fn(() => 'a valid template'),
        getTemplateByID: jest.fn(() => 'a valid template'),
        
      }),
    };


    const module: TestingModule = await Test.createTestingModule({
      controllers: [RenderingTemplatesController],
      providers: [RenderingTemplatesService, mockTemplatesProvider],
    })
    .useMocker((token)=>{
      const results = ['test1', 'test2'];
      if ( token === RenderingTemplatesService ) {
        return { findaAll: jest.fn().mockResolvedValue(results)};
      }
      if ( typeof token == 'function'){
        const mockMetadata = moduleMocker.getMetadata(token) as MockFunctionMetadata<any, any>;
        const Mock = moduleMocker.generateFromMetadata(mockMetadata);
        return new Mock();
      }
    }).compile();

    controller = module.get<RenderingTemplatesController>(RenderingTemplatesController);
  });

  it('should be defined', () => {
    expect(controller).toBeDefined();
  });

  describe('get templates by schema id', () => {
    it('should return valid string', () => {
      expect(controller.getTemplateBySchemaID(id)).toBe('a valid template');
    });
  });


});
