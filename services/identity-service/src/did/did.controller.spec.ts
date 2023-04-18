import { Test, TestingModule } from '@nestjs/testing';
import { DidController } from './did.controller';

describe('DidController', () => {
  let controller: DidController;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      controllers: [DidController],
    }).compile();

    controller = module.get<DidController>(DidController);
  });

  it('should be defined', () => {
    expect(controller).toBeDefined();
  });
});
