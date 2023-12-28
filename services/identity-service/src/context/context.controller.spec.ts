import { HttpModule } from '@nestjs/axios';
import { Test, TestingModule } from '@nestjs/testing';
import { ContextController } from './context.controller';
import { ContextService } from './context.service';
import { PrismaService } from '../utils/prisma.service';
import { ConfigService } from '@nestjs/config';

describe('ContextController', () => {
  let controller: ContextController;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      controllers: [ContextController],
      providers: [ContextService, PrismaService, ConfigService],
      imports: [HttpModule]
    }).compile();

    controller = module.get<ContextController>(ContextController);
    jest.restoreAllMocks();
  });

  it('should be defined', () => {
    expect(controller).toBeDefined();
  });

  it('should test get context', async () => {
    jest.spyOn((controller as any).contextService, "getContextById")
    .mockImplementationOnce(() => Promise.resolve({ "version": "1.0.0" }))
    const context = await controller.getContextById("abc123");
    expect(context).toBeDefined();
    expect(context).toEqual({ "version": "1.0.0" });
  });

});
