import { HttpModule } from '@nestjs/axios';
import { Test, TestingModule } from '@nestjs/testing';
import { RevocationListController } from './revocation-list.controller';
import { RevocationListService } from './revocation-list.service';
import { PrismaClient } from '@prisma/client';

describe('RevocationListController', () => {
  let controller: RevocationListController;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      imports: [HttpModule],
      controllers: [RevocationListController],
      providers: [
        RevocationListService,
        PrismaClient,
      ],
    }).compile();

    controller = module.get<RevocationListController>(RevocationListController);
  });

  it('should be defined', () => {
    expect(controller).toBeDefined();
  });
});