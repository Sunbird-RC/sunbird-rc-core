import { HttpModule } from '@nestjs/axios';
import { Test, TestingModule } from '@nestjs/testing';
import { AppController } from './app.controller';
import { AppService } from './app.service';
import { PrismaClient } from '@prisma/client';
import { TerminusModule } from '@nestjs/terminus';
import { HealthCheckUtilsService } from './credentials/utils/healthcheck.utils.service';

describe('AppController', () => {
  let appController: AppController;

  beforeEach(async () => {
    const app: TestingModule = await Test.createTestingModule({
      imports: [HttpModule, TerminusModule],
      controllers: [AppController],
      providers: [AppService, PrismaClient, HealthCheckUtilsService],
    }).compile();

    appController = app.get<AppController>(AppController);
  });

  describe('App Controller', () => {
    it('Expect controller to be defined', () => {
      expect(appController).toBeDefined()
    });
  });
});
