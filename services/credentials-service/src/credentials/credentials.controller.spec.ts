import { HttpModule } from '@nestjs/axios';
import { Test, TestingModule } from '@nestjs/testing';
import { CredentialsController } from './credentials.controller';
import { CredentialsService } from './credentials.service';
import { IdentityUtilsService } from './utils/identity.utils.service';
import { SchemaUtilsSerivce } from './utils/schema.utils.service';
import { RenderingUtilsService } from './utils/rendering.utils.service';
import { PrismaClient } from '@prisma/client';

describe('CredentialsController', () => {
  let controller: CredentialsController;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      imports: [HttpModule],
      controllers: [CredentialsController],
      providers: [
        CredentialsService,
        PrismaClient,
        IdentityUtilsService,
        SchemaUtilsSerivce,
        RenderingUtilsService,
      ],
    }).compile();

    controller = module.get<CredentialsController>(CredentialsController);
  });

  it('should be defined', () => {
    expect(controller).toBeDefined();
  });
});
