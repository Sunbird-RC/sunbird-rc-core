import { Test, TestingModule } from '@nestjs/testing';
import { AppController } from './app.controller';
import { AppService } from './app.service';
import { PrismaClient } from '@prisma/client';
import { HealthCheckUtilsService } from './credentials/utils/healthcheck.utils.service';
import { HealthCheckError, HealthCheckService, TerminusModule } from '@nestjs/terminus';
import { HttpModule, HttpService } from '@nestjs/axios';
import { AXIOS_INSTANCE_TOKEN } from '@nestjs/axios/dist/http.constants';
import axios from 'axios';
import { HealthCheckExecutor } from '@nestjs/terminus/dist/health-check/health-check-executor.service';
import { getErrorLoggerProvider } from '@nestjs/terminus/dist/health-check/error-logger/error-logger.provider';
import { getLoggerProvider } from '@nestjs/terminus/dist/health-check/logger/logger.provider';

const mockAxiosInstance = axios.create(); // Create a mock Axios instance

const mockHttpService = {
  provide: AXIOS_INSTANCE_TOKEN,
  useValue: mockAxiosInstance,
};

describe('AppController', () => {
  let appController: AppController;

  beforeAll(async () => {
    const app: TestingModule = await Test.createTestingModule({
      imports: [TerminusModule, HttpModule],
      controllers: [AppController],
      providers: [
        getLoggerProvider(),
        getErrorLoggerProvider(),
        HealthCheckExecutor,
        HealthCheckError,
        mockHttpService, HealthCheckService, AppService, PrismaClient, HealthCheckUtilsService],
    }).compile();

    appController = app.get<AppController>(AppController);
  });

  describe('App Controller', () => {
    it('Expect controller to be defined', () => {
      expect(appController).toBeDefined()
    });
  });
});
